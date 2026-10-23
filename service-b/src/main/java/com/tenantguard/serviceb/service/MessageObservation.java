package com.tenantguard.serviceb.service;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MessageObservation {
    private final String requestId;
    private final String verifiedTenant;
    private final boolean operationChecks;
    private final Queue<String> violations = new ConcurrentLinkedQueue<>();

    public MessageObservation(String requestId, String verifiedTenant, boolean operationChecks) {
        this.requestId = requestId;
        this.verifiedTenant = verifiedTenant;
        this.operationChecks = operationChecks;
    }

    public String requestId() { return requestId; }
    public String verifiedTenant() { return verifiedTenant; }
    public boolean operationChecks() { return operationChecks; }

    public void record(String code) { violations.add(code); }

    public void checkStatement(String effectiveTenant) {
        if (!operationChecks) return;
        if (verifiedTenant == null || !Objects.equals(effectiveTenant, verifiedTenant)) {
            violations.add("CONTEXT_MISMATCH");
        }
    }

    public void checkRow(String ownerTenant) {
        if (!operationChecks) return;
        if (!Objects.equals(ownerTenant, verifiedTenant)) {
            violations.add("OWNER_MISMATCH");
        }
    }

    public boolean isViolating() { return !violations.isEmpty(); }

    public String firstViolation() {
        String v = violations.peek();
        return v == null ? "NONE" : v;
    }
}