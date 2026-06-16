package com.devcrafter.Patisserie.App.services.payment;

import com.devcrafter.Patisserie.App.config.KkiapayConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
    public class KkiapayService {

        private final KkiapayConfig kkiapayConfig;
        private final ObjectMapper  objectMapper;
        private final OkHttpClient  httpClient = new OkHttpClient();

        /**
         * Verify transaction with Kkiapay API.
         * Always verify before recording payment in DB.
         */
        public KkiapayVerifyResponse verifyTransaction(
            String transactionId) {
        try {
            String url = kkiapayConfig.getBaseUrl() + "/api/v1/transactions/status";

            log.info("Verifying transaction at: {}", url);
            log.info("Transaction ID: {}", transactionId);

            Map<String, String> payload = new HashMap<>();
            payload.put("transactionId", transactionId);
            String jsonBody = objectMapper.writeValueAsString(payload);

            RequestBody body = RequestBody.create(
                    jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("X-PRIVATE-KEY",
                            kkiapayConfig.getPrivateKey())
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {

                String responseBody = response.body().string();
                log.info("Kkiapay verify [{}]: {}",
                        response.code(), responseBody);

                if (!response.isSuccessful()) {
                    log.error("Kkiapay error {}: {}",
                            response.code(), responseBody);
                    return new KkiapayVerifyResponse(
                            false,
                            "HTTP_" + response.code(),
                            BigDecimal.ZERO,
                            transactionId,
                            null
                    );
                }

                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

                // Check both status and isPaymentSucces
                // (exact spelling from Kkiapay docs)
                String status = (String) result.get("status");
                boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);
                String source = (String) result.get("source");

                Object amountObj = result.get("amount");
                BigDecimal amount = amountObj != null
                        ? new BigDecimal(amountObj.toString())
                        : BigDecimal.ZERO;

                log.info(
                        "Transaction {} — status: {}  {} amount: {}",
                        transactionId, status, amount
                );

                return new KkiapayVerifyResponse(
                        isSuccess, status,
                        amount, transactionId, source
                );
            }

        } catch (Exception e) {
            log.error("Verify failed: {}", e.getMessage());
            return new KkiapayVerifyResponse(
                    false, "ERROR",
                    BigDecimal.ZERO, transactionId, null
            );
        }
    }

    /**
     * Verify webhook signature.
     * Header name: x-kkiapay-secret
     * Secret Key
     */
    public boolean verifyWebhookSignature(
            String payload, String signature) {
        try {
            if (signature == null || signature.isBlank()) {
                log.warn("No webhook signature provided");
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    // Use Secret Key not Private Key
                    kkiapayConfig.getSecretKey().getBytes(),
                    "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            String expected = Base64.getEncoder()
                    .encodeToString(hash);

            boolean valid = expected.equals(signature);
            if (!valid) {
                log.warn(
                        "Invalid signature. " +
                                "Expected: {} Got: {}",
                        expected, signature
                );
            }
            return valid;

        } catch (Exception e) {
            log.error("Signature error: {}", e.getMessage());
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class KkiapayVerifyResponse {
        private boolean    success;
        private String     status;
        private BigDecimal amount;
        private String     transactionId;
        private String source;
    }
}
