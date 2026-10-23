package com.tenantguard.servicea.controller;

import com.tenantguard.servicea.service.TestRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private TestRunner testRunner;

    /**
     * Endpoint: GET /api/test/run-all
     * Executes all 100 test scenarios
     */
    @GetMapping("/run-all")
    public String runAllTests() {
        log.info("Test execution requested via API");
        TestRunner.TestResult result = testRunner.runAllScenarios();

        return "Test execution complete. Sent: " + result.sentScenarios + "/" + result.totalScenarios +
                ", Time: " + result.executionTimeMs + "ms, Success: " + result.success;
    }

    @GetMapping("/status")
    public String status() {
        return "Test runner ready. Call /api/test/run-all to execute all 100 scenarios";
    }
}