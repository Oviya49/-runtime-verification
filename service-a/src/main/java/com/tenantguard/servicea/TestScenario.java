package com.tenantguard.servicea;

import com.tenantguard.servicea.service.FaultInjector;

/**
 * Test scenario data structure
 */
public class TestScenario {

    public static class TestScenarioData {
        public String scenarioId;
        public String tenantId;
        public FaultInjector.FaultType faultType;
        public boolean isFaulty;

        public TestScenarioData(String scenarioId, String tenantId, FaultInjector.FaultType faultType, boolean isFaulty) {
            this.scenarioId = scenarioId;
            this.tenantId = tenantId;
            this.faultType = faultType;
            this.isFaulty = isFaulty;
        }
    }
}