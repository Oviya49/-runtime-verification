package com.tenantguard.serviceb.service;

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static String current() { return CURRENT.get(); }
    public static void set(String tenantId) { CURRENT.set(tenantId); }
    public static void clear() { CURRENT.remove(); }
}