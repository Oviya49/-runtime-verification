package com.tenantguard.serviceb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantRequest {
    private String tenantId;
    private String requestId;
    private String action;
    private String payload;
}