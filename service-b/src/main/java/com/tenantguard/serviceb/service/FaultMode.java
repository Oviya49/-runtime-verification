package com.tenantguard.serviceb.service;

public enum FaultMode {
    NONE,
    F1_MISSING_CONTEXT,
    F2_STALE_REUSE,
    F3_ASYNC_LOSS,
    F4_MISSING_FILTER
}