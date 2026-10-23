package com.tenantguard.serviceb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects metrics on fault detection performance
 * Measures: latency, recall, false positives, overhead
 */
@Slf4j
@Component
public class MetricsCollector {

    public static class DetectionMetric {
        public String faultType;
        public long detectionTimeMs;
        public boolean detected;
        public String requestId;
        public long timestamp;

        public DetectionMetric(String faultType, long detectionTimeMs, boolean detected, String requestId) {
            this.faultType = faultType;
            this.detectionTimeMs = detectionTimeMs;
            this.detected = detected;
            this.requestId = requestId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private List<DetectionMetric> metrics = new CopyOnWriteArrayList<>();

    /**
     * Record a detection metric
     */
    public void recordMetric(String faultType, long detectionTimeMs, boolean detected, String requestId) {
        metrics.add(new DetectionMetric(faultType, detectionTimeMs, detected, requestId));
    }

    /**
     * Get average detection latency per fault type
     */
    public Map<String, Double> getAverageLatency() {
        Map<String, Double> result = new HashMap<>();

        metrics.stream()
                .collect(java.util.stream.Collectors.groupingBy(m -> m.faultType))
                .forEach((faultType, metricsList) -> {
                    double avgLatency = metricsList.stream()
                            .mapToLong(m -> m.detectionTimeMs)
                            .average()
                            .orElse(0.0);
                    result.put(faultType, avgLatency);
                });

        return result;
    }

    /**
     * Get recall (percentage of faults detected)
     */
    public Map<String, Double> getRecall() {
        Map<String, Double> result = new HashMap<>();

        metrics.stream()
                .collect(java.util.stream.Collectors.groupingBy(m -> m.faultType))
                .forEach((faultType, metricsList) -> {
                    long detected = metricsList.stream()
                            .filter(m -> m.detected)
                            .count();
                    double recall = (detected * 100.0) / metricsList.size();
                    result.put(faultType, recall);
                });

        return result;
    }

    /**
     * Print metrics report
     */
    public void printReport() {
        log.info("========== METRICS REPORT ==========");
        log.info("Total metrics collected: {}", metrics.size());
        log.info("");

        log.info("--- Average Detection Latency (ms) ---");
        getAverageLatency().forEach((faultType, latency) -> {
            log.info("{}: {:.2f} ms", faultType, latency);
        });

        log.info("");
        log.info("--- Recall (Detection Rate %) ---");
        getRecall().forEach((faultType, recall) -> {
            log.info("{}: {:.2f}%", faultType, recall);
        });

        log.info("===================================");
    }

    /**
     * Export metrics as CSV for graphing
     */
    public String exportAsCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("FaultType,DetectionTimeMs,Detected,RequestId,Timestamp\n");

        for (DetectionMetric m : metrics) {
            csv.append(String.format("%s,%d,%s,%s,%d\n",
                    m.faultType, m.detectionTimeMs, m.detected, m.requestId, m.timestamp));
        }

        return csv.toString();
    }

    /**
     * Get all metrics
     */
    public List<DetectionMetric> getAllMetrics() {
        return new ArrayList<>(metrics);
    }
}