package com.tenantguard.serviceb.service;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MessageObservation {
    private final String requestId;
    private final String verifiedTenant;
    private final boolean statementChecks;
    private final boolean rowChecks;
    private final Queue<String> violations = new ConcurrentLinkedQueue<>();

    public MessageObservation(String requestId, String verifiedTenant,
                              boolean statementChecks, boolean rowChecks) {
        this.requestId = requestId;
        this.verifiedTenant = verifiedTenant;
        this.statementChecks = statementChecks;
        this.rowChecks = rowChecks;
    }

    public String requestId() { return requestId; }
    public String verifiedTenant() { return verifiedTenant; }
    public boolean statementChecks() { return statementChecks; }
    public boolean rowChecks() { return rowChecks; }

    public void record(String code) { violations.add(code); }

    public void checkStatement(String effectiveTenant) {
        if (!statementChecks) return;
        if (verifiedTenant == null || !Objects.equals(effectiveTenant, verifiedTenant)) {
            violations.add("CONTEXT_MISMATCH");
        }
    }

    public void checkRow(String ownerTenant) {
        if (!rowChecks) return;
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