package com.tenantguard.servicea;

import com.tenantguard.servicea.model.TenantRequest;
import com.tenantguard.servicea.service.FaultInjector;
import lombok.extern.slf4j.Slf4j;

/**
 * Test scenarios for TenantGuard
 * Tests all 5 fault types with multiple scenarios
 */
@Slf4j
public class TestScenarios {

    public static class Scenario {
        public String scenarioId;
        public String tenantId;
        public FaultInjector.FaultType faultType;
        public String description;
        public boolean shouldDetectFault;

        public Scenario(String scenarioId, String tenantId, FaultInjector.FaultType faultType,
                        String description, boolean shouldDetectFault) {
            this.scenarioId = scenarioId;
            this.tenantId = tenantId;
            this.faultType = faultType;
            this.description = description;
            this.shouldDetectFault = shouldDetectFault;
        }
    }

    /**
     * Generate 50 test scenarios covering all 5 fault types
     */
    public static Scenario[] generateTestScenarios() {
        Scenario[] scenarios = new Scenario[50];
        int index = 0;

        // Group 1: MISSING_HEADER (10 scenarios)
        for (int i = 0; i < 10; i++) {
            scenarios[index++] = new Scenario(
                    "MISSING_HEADER_" + i,
                    "tenant-" + (i % 3 + 1),
                    FaultInjector.FaultType.MISSING_HEADER,
                    "Missing tenant ID in message header - scenario " + i,
                    true  // Should be detected
            );
        }

        // Group 2: WRONG_TENANT (10 scenarios)
        for (int i = 0; i < 10; i++) {
            scenarios[index++] = new Scenario(
                    "WRONG_TENANT_" + i,
                    "tenant-" + (i % 3 + 1),
                    FaultInjector.FaultType.WRONG_TENANT,
                    "Wrong tenant ID swapped - scenario " + i,
                    true
            );
        }

        // Group 3: STALE_CONTEXT (10 scenarios)
        for (int i = 0; i < 10; i++) {
            scenarios[index++] = new Scenario(
                    "STALE_CONTEXT_" + i,
                    "tenant-" + (i % 3 + 1),
                    FaultInjector.FaultType.STALE_CONTEXT,
                    "Stale ThreadLocal from thread pool reuse - scenario " + i,
                    true
            );
        }

        // Group 4: MISSING_DB_FILTER (10 scenarios)
        for (int i = 0; i < 10; i++) {
            scenarios[index++] = new Scenario(
                    "MISSING_DB_FILTER_" + i,
                    "tenant-" + (i % 3 + 1),
                    FaultInjector.FaultType.MISSING_DB_FILTER,
                    "Missing tenant filter in DB query - scenario " + i,
                    true
            );
        }

        // Group 5: ASYNC_LOSS (10 scenarios)
        for (int i = 0; i < 10; i++) {
            scenarios[index++] = new Scenario(
                    "ASYNC_LOSS_" + i,
                    "tenant-" + (i % 3 + 1),
                    FaultInjector.FaultType.ASYNC_LOSS,
                    "Context loss across async RabbitMQ boundary - scenario " + i,
                    true
            );
        }

        return scenarios;
    }

    /**
     * Generate clean scenarios (no faults) - 50 scenarios
     */
    public static Scenario[] generateCleanScenarios() {
        Scenario[] scenarios = new Scenario[50];
        for (int i = 0; i < 50; i++) {
            scenarios[i] = new Scenario(
                    "CLEAN_" + i,
                    "tenant-" + (i % 3 + 1),
                    FaultInjector.FaultType.NONE,
                    "Clean request without faults - scenario " + i,
                    false  // Should NOT be detected as faulty
            );
        }
        return scenarios;
    }

    /**
     * Print test plan
     */
    public static void printTestPlan() {
        log.info("========== TEST PLAN ==========");
        log.info("Total Faulty Scenarios: 50 (10 per fault type)");
        log.info("Total Clean Scenarios: 50 (no faults)");
        log.info("Total Scenarios: 100");
        log.info("");
        log.info("Fault Types:");
        log.info("1. MISSING_HEADER (10) - No tenant ID in header");
        log.info("2. WRONG_TENANT (10) - Wrong tenant ID swapped");
        log.info("3. STALE_CONTEXT (10) - Stale ThreadLocal from thread reuse");
        log.info("4. MISSING_DB_FILTER (10) - Missing tenant filter in DB");
        log.info("5. ASYNC_LOSS (10) - Context loss across async boundaries");
        log.info("==============================");
    }
}