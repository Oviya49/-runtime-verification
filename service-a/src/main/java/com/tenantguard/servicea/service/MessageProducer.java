package com.tenantguard.servicea.service;

import com.tenantguard.servicea.config.RabbitMQConfig;
import com.tenantguard.servicea.model.TenantRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private FaultInjector faultInjector;

    @Value("${tenantguard.signing.key:dev-only-key}")
    private String signingKey;

    public void sendTenantRequest(String tenantId, TenantRequest request, FaultInjector.FaultType faultType) {
        request.setTenantId(tenantId);

        if (faultType != FaultInjector.FaultType.NONE) {
            request = faultInjector.injectFault(request, faultType);
        }

        String producedAt = Instant.now().toString();
        String signature = RequestSigner.sign(signingKey, tenantId, request.getRequestId(), producedAt);

        log.info("Sending message - Tenant: {}, RequestID: {}, Fault: {}",
                tenantId, request.getRequestId(), faultType);

        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, request, message -> {
            message.getMessageProperties().setHeader("X-Tenant-ID", tenantId);
            message.getMessageProperties().setHeader("X-Produced-At", producedAt);
            message.getMessageProperties().setHeader("X-Auth-Sig", signature);
            return message;
        });
    }

    public void sendTenantRequest(String tenantId, TenantRequest request) {
        sendTenantRequest(tenantId, request, FaultInjector.FaultType.NONE);
    }
}