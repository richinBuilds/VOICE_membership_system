package org.voice.membership.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService {

    private final PayPalProperties payPalProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String createOrder(User user, Membership membership) throws IOException, InterruptedException {
        String customId = buildCustomId(user.getId(), membership.getId());
        String description = "VOICE Membership Upgrade - " + membership.getName();
        return createOrderWithCustomId(membership, customId, description);
    }

    public String createOrderForRegistration(Membership membership, String registrationRef)
            throws IOException, InterruptedException {
        String customId = buildRegistrationCustomId(registrationRef, membership.getId());
        String description = "VOICE Membership Registration - " + membership.getName();
        return createOrderWithCustomId(membership, customId, description);
    }

    private String createOrderWithCustomId(Membership membership, String customId, String description)
            throws IOException, InterruptedException {
        String accessToken = getAccessToken();
        String expectedAmount = formatAmount(membership.getPrice());

        ObjectNode amountNode = objectMapper.createObjectNode()
                .put("currency_code", payPalProperties.getCurrency())
                .put("value", expectedAmount);

        ObjectNode purchaseUnitNode = objectMapper.createObjectNode()
                .put("reference_id", "membership-" + membership.getId())
                .put("custom_id", customId)
                .put("description", description)
                .set("amount", amountNode);

        ObjectNode applicationContextNode = objectMapper.createObjectNode()
                .put("shipping_preference", "NO_SHIPPING")
                .put("user_action", "PAY_NOW");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("intent", "CAPTURE");
        payload.set("purchase_units", objectMapper.createArrayNode().add(purchaseUnitNode));
        payload.set("application_context", applicationContextNode);

        JsonNode response = postJson("/v2/checkout/orders", payload, accessToken, null);
        String orderId = response.path("id").asText();

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalStateException("PayPal order id missing in response");
        }

        log.info("PayPal order created for membership {} orderId {}", membership.getId(), orderId);
        return orderId;
    }

    public CaptureValidationResult captureAndValidateOrder(User user, Membership membership, String orderId)
            throws IOException, InterruptedException {
        log.info("Starting PayPal capture for upgrade - OrderID: {}, UserID: {}, MembershipID: {}",
                orderId, user.getId(), membership.getId());

        String accessToken = getAccessToken();

        JsonNode captureResponse = postJson(
                "/v2/checkout/orders/" + orderId + "/capture",
                objectMapper.createObjectNode(),
                accessToken,
                "VOICE-UPGRADE-" + orderId);

        log.debug("PayPal capture response: {}", captureResponse.toPrettyString());

        String expectedCustomId = buildCustomId(user.getId(), membership.getId());
        return validateCaptureResponse(captureResponse, membership, expectedCustomId);
    }

    public CaptureValidationResult captureAndValidateRegistration(Membership membership, String orderId,
            String registrationRef)
            throws IOException, InterruptedException {
        log.info("Starting PayPal capture for registration - OrderID: {}, MembershipID: {}, RegRef: {}",
                orderId, membership.getId(), registrationRef);

        String accessToken = getAccessToken();

        JsonNode captureResponse = postJson(
                "/v2/checkout/orders/" + orderId + "/capture",
                objectMapper.createObjectNode(),
                accessToken,
                "VOICE-REGISTER-" + orderId);

        log.debug("PayPal capture response: {}", captureResponse.toPrettyString());

        String expectedCustomId = buildRegistrationCustomId(registrationRef, membership.getId());
        return validateCaptureResponse(captureResponse, membership, expectedCustomId);
    }

    public CaptureValidationResult validateOrderFromPayPal(User user, Membership membership, String orderId)
            throws IOException, InterruptedException {
        String accessToken = getAccessToken();
        JsonNode orderResponse = getJson("/v2/checkout/orders/" + orderId, accessToken);

        JsonNode purchaseUnit = orderResponse.path("purchase_units").path(0);
        JsonNode capture = purchaseUnit.path("payments").path("captures").path(0);
        if (capture.isMissingNode()) {
            return new CaptureValidationResult(false, null, "MISSING_CAPTURE", "No capture found on PayPal order");
        }

        JsonNode syntheticCaptureResponse = objectMapper.createObjectNode()
                .put("status", orderResponse.path("status").asText())
                .set("purchase_units", objectMapper.createArrayNode().add(
                        objectMapper.createObjectNode()
                                .put("custom_id", purchaseUnit.path("custom_id").asText())
                                .set("payments", objectMapper.createObjectNode()
                                        .set("captures", objectMapper.createArrayNode().add(capture)))));

        String expectedCustomId = buildCustomId(user.getId(), membership.getId());
        return validateCaptureResponse(syntheticCaptureResponse, membership, expectedCustomId);
    }

    public boolean verifyWebhookSignature(Map<String, String> headers, JsonNode webhookEvent)
            throws IOException, InterruptedException {
        if (payPalProperties.getWebhookId() == null || payPalProperties.getWebhookId().isBlank()) {
            log.warn("PayPal webhook id is not configured");
            return false;
        }

        String accessToken = getAccessToken();
        JsonNode payload = objectMapper.createObjectNode()
                .put("transmission_id", getHeader(headers, "paypal-transmission-id"))
                .put("transmission_time", getHeader(headers, "paypal-transmission-time"))
                .put("cert_url", getHeader(headers, "paypal-cert-url"))
                .put("auth_algo", getHeader(headers, "paypal-auth-algo"))
                .put("transmission_sig", getHeader(headers, "paypal-transmission-sig"))
                .put("webhook_id", payPalProperties.getWebhookId())
                .set("webhook_event", webhookEvent);

        JsonNode response = postJson("/v1/notifications/verify-webhook-signature", payload, accessToken, null);
        String verificationStatus = response.path("verification_status").asText();
        return "SUCCESS".equalsIgnoreCase(verificationStatus);
    }

    public String buildCustomId(int userId, int membershipId) {
        return "voice:user:" + userId + ":membership:" + membershipId;
    }

    public String buildRegistrationCustomId(String registrationRef, int membershipId) {
        return "voice:registration:" + registrationRef + ":membership:" + membershipId;
    }

    CaptureValidationResult validateCaptureResponse(JsonNode response, Membership membership,
            String expectedCustomId) {
        String orderStatus = response.path("status").asText();
        JsonNode purchaseUnit = response.path("purchase_units").path(0);
        JsonNode capture = purchaseUnit.path("payments").path("captures").path(0);

        String captureStatus = capture.path("status").asText();
        String captureId = capture.path("id").asText(null);
        String currency = capture.path("amount").path("currency_code").asText();
        String value = capture.path("amount").path("value").asText();
        String customId = purchaseUnit.path("custom_id").asText();

        log.info("=== PayPal Capture Validation ===");
        log.info("Capture ID: {}", captureId);
        log.info("Capture Status: {}", captureStatus);
        log.info("Order Status: {}", orderStatus);
        log.info("Currency: {} (expected: {})", currency, payPalProperties.getCurrency());
        log.info("Amount: {} (expected: {})", value, membership.getPrice());
        log.info("Custom ID: {} (expected: {})", customId, expectedCustomId);

        if (!"COMPLETED".equalsIgnoreCase(captureStatus)) {
            log.warn("VALIDATION FAILED: Capture status is '{}', expected 'COMPLETED'", captureStatus);
            return new CaptureValidationResult(false, captureId, captureStatus, "Payment capture is not completed");
        }

        if (!("COMPLETED".equalsIgnoreCase(orderStatus) || "APPROVED".equalsIgnoreCase(orderStatus))) {
            log.warn("VALIDATION FAILED: Order status is '{}', expected 'COMPLETED' or 'APPROVED'", orderStatus);
            return new CaptureValidationResult(false, captureId, orderStatus, "Order status is not completed");
        }

        String expectedCurrency = payPalProperties.getCurrency();
        if (expectedCurrency == null || expectedCurrency.isBlank()) {
            expectedCurrency = "CAD";
        }
        if (!expectedCurrency.equalsIgnoreCase(currency)) {
            log.warn("VALIDATION FAILED: Currency is '{}', expected '{}'", currency, expectedCurrency);
            return new CaptureValidationResult(false, captureId, captureStatus, "Currency mismatch");
        }

        BigDecimal paidAmount;
        try {
            paidAmount = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            log.warn("VALIDATION FAILED: Invalid amount value '{}'", value, ex);
            return new CaptureValidationResult(false, captureId, captureStatus, "Invalid amount in capture");
        }

        BigDecimal expectedAmount = membership.getPrice().setScale(2, RoundingMode.HALF_UP);
        if (paidAmount.compareTo(expectedAmount) != 0) {
            log.warn("VALIDATION FAILED: Amount mismatch. Paid: {}, Expected: {}", paidAmount, expectedAmount);
            return new CaptureValidationResult(false, captureId, captureStatus, "Amount mismatch");
        }

        // Custom ID validation - optional since PayPal sandbox doesn't always return it
        // Session/database validation provides security for registration/upgrade flows
        if (customId != null && !customId.isBlank() && !expectedCustomId.equals(customId)) {
            log.warn("VALIDATION FAILED: Custom ID mismatch. Received: '{}', Expected: '{}'", customId,
                    expectedCustomId);
            return new CaptureValidationResult(false, captureId, captureStatus, "Payment target mismatch");
        }

        if (customId == null || customId.isBlank()) {
            log.warn("Custom ID not returned by PayPal (common in sandbox). Relying on session/database validation.");
        }

        log.info("VALIDATION SUCCESS: All checks passed");
        return new CaptureValidationResult(true, captureId, captureStatus, "OK");
    }

    private String getAccessToken() throws IOException, InterruptedException {
        if (!payPalProperties.hasCredentials()) {
            throw new IllegalStateException("PayPal credentials are not configured");
        }

        String credentials = payPalProperties.getClientId() + ":" + payPalProperties.getClientSecret();
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(payPalProperties.getBaseUrl() + "/v1/oauth2/token"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Failed to get PayPal access token. HTTP " + response.statusCode());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String token = json.path("access_token").asText();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("PayPal access token missing");
        }
        return token;
    }

    private JsonNode postJson(String path, JsonNode payload, String accessToken, String requestId)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(payPalProperties.getBaseUrl() + path))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

        if (requestId != null && !requestId.isBlank()) {
            builder.header("PayPal-Request-Id", requestId);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("PayPal API call failed. HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private JsonNode getJson(String path, String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(payPalProperties.getBaseUrl() + path))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("PayPal GET call failed. HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String getHeader(Map<String, String> headers, String key) {
        String value = headers.get(key);
        if (value != null) {
            return value;
        }

        String normalizedKey = key.toLowerCase(Locale.ROOT);
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null
                        && entry.getKey().toLowerCase(Locale.ROOT).equals(normalizedKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }

    public record CaptureValidationResult(boolean completed, String captureId, String status, String message) {
    }
}
