package com.tenantguard.serviceb.service;

import com.tenantguard.serviceb.config.RabbitMQConfig;
import com.tenantguard.serviceb.model.TenantRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class MessageConsumer {

    private final TenantRowStore store;
    private final MetricsCollector metricsCollector;
    private final ThreadPoolTaskExecutor consumerExecutor;
    private final FaultMode mode;
    private final String signingKey;
    private final boolean operationChecks;

    public MessageConsumer(TenantRowStore store,
                           MetricsCollector metricsCollector,
                           ThreadPoolTaskExecutor consumerExecutor,
                           @Value("${tenantguard.fault.mode:NONE}") String mode,
                           @Value("${tenantguard.signing.key:dev-only-key}") String signingKey,
                           @Value("${tenantguard.detector.mode:VERIFIER}") String detectorMode) {
        this.store = store;
        this.metricsCollector = metricsCollector;
        this.consumerExecutor = consumerExecutor;
        this.mode = FaultMode.valueOf(mode);
        this.signingKey = signingKey;
        this.operationChecks = !"HEADER_ONLY".equals(detectorMode);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeTenantRequest(TenantRequest request,
                                     @Header(name = "X-Tenant-ID", required = false) String headerTenant,
                                     @Header(name = "X-Produced-At", required = false) String producedAt,
                                     @Header(name = "X-Auth-Sig", required = false) String signature) {
        long start = System.nanoTime();
        String verifiedTenant = verifyTenant(headerTenant, request.getRequestId(), producedAt, signature);
        MessageObservation obs = new MessageObservation(request.getRequestId(), verifiedTenant, operationChecks);
        if (verifiedTenant == null) {
            obs.record(headerTenant == null ? "MISSING_CONTEXT" : "INVALID_SIGNATURE");
        }
        if (headerTenant != null && !headerTenant.equals(request.getTenantId())) {
            obs.record("HEADER_BODY_MISMATCH");
        }

        log.info("Message received - RequestID: {}, Header Tenant: {}, Signature valid: {}, Mode: {}, Operation checks: {}",
                request.getRequestId(), headerTenant, verifiedTenant != null, mode, operationChecks);

        if (mode != FaultMode.F1_MISSING_CONTEXT) {
            TenantContext.set(verifiedTenant);
        }
        try {
            consumerExecutor.submit(() -> run(obs)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            obs.record("EXECUTION_ERROR");
        } catch (ExecutionException e) {
            obs.record("EXECUTION_ERROR");
            log.error("Processing failed for RequestID: {}", request.getRequestId(), e);
        } finally {
            TenantContext.clear();
        }

        long elapsedUs = (System.nanoTime() - start) / 1_000;
        metricsCollector.recordMetric(obs.firstViolation(), elapsedUs, obs.isViolating(), request.getRequestId());

        if (obs.isViolating()) {
            log.error("ALERT - RequestID: {}, Violation: {}", request.getRequestId(), obs.firstViolation());
        } else {
            log.info("Safe to process request for tenant: {}", verifiedTenant);
        }
    }

    private String verifyTenant(String tenant, String requestId, String producedAt, String signature) {
        if (tenant == null || producedAt == null || signature == null) {
            return null;
        }
        String expected = RequestSigner.sign(signingKey, tenant, requestId, producedAt);
        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
        return valid ? tenant : null;
    }

    private void run(MessageObservation obs) {
        String verified = obs.verifiedTenant();
        String leftover = TenantContext.current();
        if (obs.operationChecks() && leftover != null && !leftover.equals(verified)) {
            obs.record("STALE_LEFTOVER");
        }

        switch (mode) {
            case F2_STALE_REUSE -> {
                TenantContext.set(verified);
                store.visibleRows(obs);
            }
            case F3_ASYNC_LOSS -> store.visibleRows(obs);
            case F1_MISSING_CONTEXT -> {
                try {
                    store.visibleRows(obs);
                } finally {
                    TenantContext.clear();
                }
            }
            default -> {
                TenantContext.set(verified);
                try {
                    store.visibleRows(obs);
                } finally {
                    TenantContext.clear();
                }
            }
        }
    }
}