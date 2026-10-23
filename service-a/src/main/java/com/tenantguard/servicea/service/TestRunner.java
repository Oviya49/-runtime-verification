package com.tenantguard.servicea.service;

import com.tenantguard.servicea.model.TenantRequest;
import com.tenantguard.servicea.TestScenario;
import com.tenantguard.servicea.TestScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates execution of all 100 test scenarios
 * - Sends requests with different fault types
 * - Measures execution time
 * - Tracks success/failure
 */
@Slf4j
@Service
public class TestRunner {

    @Autowired
    private MessageProducer messageProducer;

    public static class TestResult {
        public int totalScenarios;
        public int sentScenarios;
        public long executionTimeMs;
        public boolean success;

        public TestResult(int totalScenarios, int sentScenarios, long executionTimeMs, boolean success) {
            this.totalScenarios = totalScenarios;
            this.sentScenarios = sentScenarios;
            this.executionTimeMs = executionTimeMs;
            this.success = success;
        }
    }

    /**
     * Run all 100 test scenarios
     * 50 faulty scenarios (10 per fault type) + 50 clean scenarios
     */
    public TestResult runAllScenarios() {
        long startTime = System.currentTimeMillis();
        int sentCount = 0;

        log.info("========== STARTING TEST EXECUTION ==========");
        log.info("Total scenarios to run: 100");
        log.info("Faulty scenarios: 50 (10 per fault type)");
        log.info("Clean scenarios: 50");
        log.info("");

        try {
            // Get all scenarios
            TestScenario.TestScenarioData[] faultyScenariosData = generateFaultyScenarios();
            TestScenario.TestScenarioData[] cleanScenariosData = generateCleanScenarios();

            // Execute faulty scenarios
            log.info("Executing 50 faulty scenarios...");
            sentCount += executeScenariosSequentially(faultyScenariosData);

            // Small delay between faulty and clean scenarios
            Thread.sleep(1000);

            // Execute clean scenarios
            log.info("Executing 50 clean scenarios...");
            sentCount += executeScenariosSequentially(cleanScenariosData);

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("");
            log.info("========== TEST EXECUTION COMPLETE ==========");
            log.info("Total scenarios: 100");
            log.info("Scenarios sent: {}", sentCount);
            log.info("Execution time: {} ms ({} sec)", executionTime, executionTime / 1000);
            log.info("==========================================");

            return new TestResult(100, sentCount, executionTime, sentCount == 100);

        } catch (Exception e) {
            log.error("Error during test execution", e);
            long executionTime = System.currentTimeMillis() - startTime;
            return new TestResult(100, sentCount, executionTime, false);
        }
    }

    /**
     * Generate 50 faulty scenarios (10 per fault type)
     */
    private TestScenario.TestScenarioData[] generateFaultyScenarios() {
        TestScenario.TestScenarioData[] scenarios = new TestScenario.TestScenarioData[50];
        int index = 0;

        // 10 scenarios for each fault type
        FaultInjector.FaultType[] faultTypes = {
                FaultInjector.FaultType.MISSING_HEADER,
                FaultInjector.FaultType.WRONG_TENANT,
                FaultInjector.FaultType.STALE_CONTEXT,
                FaultInjector.FaultType.MISSING_DB_FILTER,
                FaultInjector.FaultType.ASYNC_LOSS
        };

        for (FaultInjector.FaultType faultType : faultTypes) {
            for (int i = 0; i < 10; i++) {
                scenarios[index++] = new TestScenario.TestScenarioData(
                        faultType.name() + "_" + i,
                        "tenant-" + (i % 3 + 1),
                        faultType,
                        true  // isFaulty
                );
            }
        }

        return scenarios;
    }

    /**
     * Generate 50 clean scenarios (no faults)
     */
    private TestScenario.TestScenarioData[] generateCleanScenarios() {
        TestScenario.TestScenarioData[] scenarios = new TestScenario.TestScenarioData[50];

        for (int i = 0; i < 50; i++) {
            scenarios[i] = new TestScenario.TestScenarioData(
                    "CLEAN_" + i,
                    "tenant-" + (i % 3 + 1),
                    FaultInjector.FaultType.NONE,
                    false  // isClean
            );
        }

        return scenarios;
    }

    /**
     * Execute scenarios sequentially with delay
     */
    private int executeScenariosSequentially(TestScenario.TestScenarioData[] scenarios) throws InterruptedException {
        int sentCount = 0;

        for (TestScenario.TestScenarioData scenario : scenarios) {
            try {
                sendScenario(scenario);
                sentCount++;

                // Small delay between requests (50ms)
                Thread.sleep(50);

            } catch (Exception e) {
                log.error("Error sending scenario: {}", scenario.scenarioId, e);
            }
        }

        return sentCount;
    }

    /**
     * Send a single test scenario
     */
    private void sendScenario(TestScenario.TestScenarioData scenario) {
        TenantRequest request = new TenantRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setTenantId(scenario.tenantId);
        request.setAction("test-action");
        request.setPayload("Test payload for scenario: " + scenario.scenarioId);

        log.debug("Sending scenario: {} - Fault: {}", scenario.scenarioId, scenario.faultType);

        messageProducer.sendTenantRequest(scenario.tenantId, request, scenario.faultType);
    }
}
