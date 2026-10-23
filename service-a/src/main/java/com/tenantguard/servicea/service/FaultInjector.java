package com.tenantguard.servicea.service;

import com.tenantguard.servicea.model.TenantRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deliberately injects faults into messages for testing
 * The 5 fault types we test:
 * 1. MISSING_HEADER - Remove tenant ID from message header
 * 2. WRONG_TENANT - Swap tenant ID with a different one
 * 3. STALE_CONTEXT - Simulate stale ThreadLocal from thread pool reuse
 * 4. MISSING_DB_FILTER - Simulate forgetting to filter by tenant in DB query
 * 5. ASYNC_LOSS - Simulate context loss across async boundaries
 */
@Slf4j
@Component
public class FaultInjector {

    public enum FaultType {
        NONE,                    // No fault
        MISSING_HEADER,         // Fault 1: Remove tenant header
        WRONG_TENANT,           // Fault 2: Wrong tenant ID
        STALE_CONTEXT,          // Fault 3: Stale ThreadLocal
        MISSING_DB_FILTER,      // Fault 4: Missing tenant filter in DB
        ASYNC_LOSS              // Fault 5: Context loss across async
    }

    private static final String[] TENANT_IDS = {"tenant-1", "tenant-2", "tenant-3"};

    /**
     * Inject a fault into the request
     * Returns modified request with fault applied
     */
    public TenantRequest injectFault(TenantRequest request, FaultType faultType) {
        switch (faultType) {
            case MISSING_HEADER:
                return injectMissingHeader(request);
            case WRONG_TENANT:
                return injectWrongTenant(request);
            case STALE_CONTEXT:
                return injectStaleContext(request);
            case MISSING_DB_FILTER:
                return injectMissingDBFilter(request);
            case ASYNC_LOSS:
                return injectAsyncLoss(request);
            case NONE:
            default:
                return request;
        }
    }

    /**
     * Fault 1: Remove tenant ID from the request
     * Simulates: Header stripped during message routing
     */
    private TenantRequest injectMissingHeader(TenantRequest request) {
        log.warn("FAULT INJECTED: MISSING_HEADER - Removing tenant ID from request");
        request.setTenantId(null);  // Simulate missing header
        return request;
    }

    /**
     * Fault 2: Swap tenant ID with a different tenant
     * Simulates: Bug where wrong tenant ID is used
     */
    private TenantRequest injectWrongTenant(TenantRequest request) {
        String originalTenant = request.getTenantId();
        String wrongTenant = TENANT_IDS[(int)(Math.random() * TENANT_IDS.length)];

        // Make sure we pick a different tenant
        while (wrongTenant.equals(originalTenant)) {
            wrongTenant = TENANT_IDS[(int)(Math.random() * TENANT_IDS.length)];
        }

        log.warn("FAULT INJECTED: WRONG_TENANT - Changing tenant from {} to {}", originalTenant, wrongTenant);
        request.setTenantId(wrongTenant);
        return request;
    }

    /**
     * Fault 3: Stale ThreadLocal context
     * Simulates: Thread pool reuse causing old context to leak
     */
    private TenantRequest injectStaleContext(TenantRequest request) {
        log.warn("FAULT INJECTED: STALE_CONTEXT - Thread pool reused, stale context present");
        // Mark the request as having stale context
        request.setPayload(request.getPayload() + "[STALE_THREADLOCAL]");
        return request;
    }

    /**
     * Fault 4: Missing DB filter
     * Simulates: Developer forgot to add WHERE tenant_id = ? in query
     */
    private TenantRequest injectMissingDBFilter(TenantRequest request) {
        log.warn("FAULT INJECTED: MISSING_DB_FILTER - DB query executed without tenant filter");
        request.setPayload(request.getPayload() + "[NO_TENANT_FILTER]");
        return request;
    }

    /**
     * Fault 5: Async context loss
     * Simulates: Context lost when crossing RabbitMQ async boundaries
     */
    private TenantRequest injectAsyncLoss(TenantRequest request) {
        log.warn("FAULT INJECTED: ASYNC_LOSS - Context lost across async RabbitMQ boundary");
        request.setPayload(request.getPayload() + "[ASYNC_CONTEXT_LOST]");
        return request;
    }
}