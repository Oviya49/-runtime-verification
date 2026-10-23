package com.tenantguard.servicea.controller;

import com.tenantguard.servicea.model.TenantRequest;
import com.tenantguard.servicea.service.FaultInjector;
import com.tenantguard.servicea.service.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/request")
public class RequestController {

    @Autowired
    private MessageProducer messageProducer;

    /**
     * Endpoint: POST /api/request/send
     * Body: {"tenantId": "tenant-1", "action": "fetch-orders"}
     * Query param: ?fault=MISSING_HEADER (or WRONG_TENANT, STALE_CONTEXT, etc.)
     */
    @PostMapping("/send")
    public String sendRequest(
            @RequestBody TenantRequest request,
            @RequestParam(required = false, defaultValue = "NONE") String fault) {

        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);

        String tenantId = request.getTenantId();

        // Parse fault type
        FaultInjector.FaultType faultType = FaultInjector.FaultType.valueOf(fault);

        messageProducer.sendTenantRequest(tenantId, request, faultType);

        return "Request sent. RequestID: " + requestId + ", TenantID: " + tenantId + ", Fault: " + fault;
    }

    @GetMapping("/health")
    public String health() {
        return "Service A is running";
    }
}