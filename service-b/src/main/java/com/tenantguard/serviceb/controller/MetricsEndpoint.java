package com.tenantguard.serviceb.controller;

import com.tenantguard.serviceb.service.MetricsCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/metrics")
public class MetricsEndpoint {

    @Autowired
    private MetricsCollector metricsCollector;

    /**
     * Get average detection latency per fault type
     */
    @GetMapping("/latency")
    public Map<String, Object> getLatency() {
        metricsCollector.printReport();

        Map<String, Object> response = new HashMap<>();
        response.put("averageLatencyMs", metricsCollector.getAverageLatency());
        response.put("recall", metricsCollector.getRecall());
        response.put("totalMetrics", metricsCollector.getAllMetrics().size());

        return response;
    }

    /**
     * Export metrics as CSV
     */
    @GetMapping("/export-csv")
    public String exportMetrics() {
        return metricsCollector.exportAsCSV();
    }

    /**
     * Get all metrics
     */
    @GetMapping("/all")
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("metricsCount", metricsCollector.getAllMetrics().size());
        response.put("metrics", metricsCollector.getAllMetrics());
        return response;
    }

    @GetMapping("/health")
    public String health() {
        return "Metrics endpoint ready";
    }
}