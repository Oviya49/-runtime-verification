package com.tenantguard.serviceb.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class RequestSigner {

    private RequestSigner() {}

    public static String sign(String key, String tenantId, String requestId, String producedAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String data = tenantId + "|" + requestId + "|" + producedAt;
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Signing failed", e);
        }
    }
}