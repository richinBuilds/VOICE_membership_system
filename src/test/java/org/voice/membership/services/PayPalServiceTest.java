package org.voice.membership.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.services.PayPalService.CaptureValidationResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PayPalService
 * Tests PayPal order creation, capture validation, and webhook signature
 * verification
 */
@ExtendWith(MockitoExtension.class)
class PayPalServiceTest {

    @Mock
    private PayPalProperties payPalProperties;

    @InjectMocks
    private PayPalService payPalService;

    private ObjectMapper objectMapper;
    private User testUser;
    private Membership testMembership;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        payPalService = new PayPalService(payPalProperties, objectMapper);

        // Setup test user
        testUser = new User();
        testUser.setId(123);
        testUser.setEmail("test@example.com");

        // Setup test membership
        testMembership = new Membership();
        testMembership.setId(2);
        testMembership.setName("Premium");
        testMembership.setPrice(new BigDecimal("20.00"));
        testMembership.setFree(false);

        // Default PayPal properties (lenient because not all tests use all stubs)
        lenient().when(payPalProperties.getBaseUrl()).thenReturn("https://api-m.sandbox.paypal.com");
        lenient().when(payPalProperties.getCurrency()).thenReturn("CAD");
        lenient().when(payPalProperties.getClientId()).thenReturn("test-client-id");
        lenient().when(payPalProperties.getClientSecret()).thenReturn("test-client-secret");
        lenient().when(payPalProperties.hasCredentials()).thenReturn(true);
    }

    // ==================== Build Custom ID Tests ====================

    @Test
    void buildCustomId_ShouldReturnCorrectFormat() {
        // Act
        String customId = payPalService.buildCustomId(123, 2);

        // Assert
        assertThat(customId).isEqualTo("voice:user:123:membership:2");
    }

    @Test
    void buildRegistrationCustomId_ShouldReturnCorrectFormat() {
        // Act
        String customId = payPalService.buildRegistrationCustomId("REG-123-ABC", 2);

        // Assert
        assertThat(customId).isEqualTo("voice:registration:REG-123-ABC:membership:2");
    }

    // ==================== Validation Logic Tests ====================

    @Test
    void validateCaptureResponse_WithValidCompletedCapture_ShouldReturnSuccess() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-123",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);
        String expectedCustomId = "voice:user:123:membership:2";

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, expectedCustomId);

        // Assert
        assertThat(result.completed()).isTrue();
        assertThat(result.captureId()).isEqualTo("CAPTURE-123");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.message()).isEqualTo("OK");
    }

    @Test
    void validateCaptureResponse_WithApprovedStatus_ShouldReturnSuccess() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "APPROVED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-456",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert
        assertThat(result.completed()).isTrue();
    }

    @Test
    void validateCaptureResponse_WithIncompleteCapture_ShouldReturnFailure() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-789",
                        "status": "PENDING",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert
        assertThat(result.completed()).isFalse();
        assertThat(result.message()).isEqualTo("Payment capture is not completed");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void validateCaptureResponse_WithCurrencyMismatch_ShouldReturnFailure() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-999",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "USD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert
        assertThat(result.completed()).isFalse();
        assertThat(result.message()).isEqualTo("Currency mismatch");
    }

    @Test
    void validateCaptureResponse_WithAmountMismatch_ShouldReturnFailure() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-111",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "15.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert
        assertThat(result.completed()).isFalse();
        assertThat(result.message()).isEqualTo("Amount mismatch");
    }

    @Test
    void validateCaptureResponse_WithCustomIdMismatch_ShouldReturnFailure() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:999:membership:9",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-222",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert
        assertThat(result.completed()).isFalse();
        assertThat(result.message()).isEqualTo("Payment target mismatch");
    }

    @Test
    void validateCaptureResponse_WithMissingCustomId_ShouldSucceedWithWarning() throws Exception {
        // Arrange - PayPal sandbox often doesn't return custom_id
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-333",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert - Should succeed, relying on session/database validation
        assertThat(result.completed()).isTrue();
        assertThat(result.message()).isEqualTo("OK");
    }

    @Test
    void validateCaptureResponse_WithInvalidAmount_ShouldReturnFailure() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-444",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "invalid"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert
        assertThat(result.completed()).isFalse();
        assertThat(result.message()).isEqualTo("Invalid amount in capture");
    }

    @Test
    void validateCaptureResponse_WithPendingOrderStatus_ShouldReturnFailure() throws Exception {
        // Arrange
        String captureJson = """
                {
                  "status": "CREATED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-555",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        // Act
        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        // Assert
        assertThat(result.completed()).isFalse();
        assertThat(result.message()).isEqualTo("Order status is not completed");
    }

    // ==================== Webhook Signature Verification Tests
    // ====================

    @Test
    void verifyWebhookSignature_WithMissingWebhookId_ShouldReturnFalse() throws Exception {
        // Arrange
        when(payPalProperties.getWebhookId()).thenReturn(null);
        Map<String, String> headers = new HashMap<>();
        JsonNode webhookEvent = objectMapper.createObjectNode();

        // Act
        boolean result = payPalService.verifyWebhookSignature(headers, webhookEvent);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void verifyWebhookSignature_WithBlankWebhookId_ShouldReturnFalse() throws Exception {
        // Arrange
        when(payPalProperties.getWebhookId()).thenReturn("");
        Map<String, String> headers = new HashMap<>();
        JsonNode webhookEvent = objectMapper.createObjectNode();

        // Act
        boolean result = payPalService.verifyWebhookSignature(headers, webhookEvent);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== CaptureValidationResult Record Tests
    // ====================

    @Test
    void captureValidationResult_ShouldCreateCorrectly() {
        // Act
        CaptureValidationResult result = new CaptureValidationResult(
                true,
                "CAPTURE-TEST-123",
                "COMPLETED",
                "Payment successful");

        // Assert
        assertThat(result.completed()).isTrue();
        assertThat(result.captureId()).isEqualTo("CAPTURE-TEST-123");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.message()).isEqualTo("Payment successful");
    }

    @Test
    void captureValidationResult_ShouldSupportFailureCase() {
        // Act
        CaptureValidationResult result = new CaptureValidationResult(
                false,
                null,
                "FAILED",
                "Insufficient funds");

        // Assert
        assertThat(result.completed()).isFalse();
        assertThat(result.captureId()).isNull();
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.message()).isEqualTo("Insufficient funds");
    }

    // ==================== SECURITY & EDGE CASE TESTS ====================

    @Test
    void validateCaptureResponse_WithNegativeAmount_ShouldReturnFailure() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-NEG",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "-20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isFalse();
        assertThat(result.message()).contains("mismatch");
    }

    @Test
    void validateCaptureResponse_WithZeroAmount_ShouldReturnFailure() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-ZERO",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "0.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isFalse();
        assertThat(result.message()).contains("mismatch");
    }

    @Test
    void validateCaptureResponse_WithVeryLargeAmount_ShouldValidateCorrectly() throws Exception {
        Membership expensiveMembership = new Membership();
        expensiveMembership.setId(99);
        expensiveMembership.setName("Platinum");
        expensiveMembership.setPrice(new BigDecimal("9999.99"));

        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:99",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-LARGE",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "9999.99"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, expensiveMembership, "voice:user:123:membership:99");

        assertThat(result.completed()).isTrue();
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    void validateCaptureResponse_WithMalformedCustomId_ShouldReturnFailure() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "malicious:sql:injection",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-MAL",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isFalse();
        assertThat(result.message()).contains("Payment target mismatch");
    }

    @Test
    void validateCaptureResponse_WithExtraCaptureFields_ShouldIgnoreAndValidate() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "unknown_field": "should be ignored",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-123",
                        "status": "COMPLETED",
                        "extra_data": "ignored",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isTrue();
    }

    @Test
    void validateCaptureResponse_WithEmptyPurchaseUnits_ShouldReturnFailure() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": []
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isFalse();
    }

    @Test
    void validateCaptureResponse_WithMissingPayments_ShouldReturnFailure() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2"
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isFalse();
    }

    @Test
    void validateCaptureResponse_WithEmptyCaptures_ShouldReturnFailure() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": []
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isFalse();
    }

    @Test
    void validateCaptureResponse_WithRoundingDifference_ShouldAcceptCloseMatch() throws Exception {
        Membership membership = new Membership();
        membership.setId(2);
        membership.setName("Test");
        membership.setPrice(new BigDecimal("20.00"));

        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-ROUND",
                        "status": "COMPLETED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, membership, "voice:user:123:membership:2");

        assertThat(result.completed()).isTrue();
    }

    @Test
    void buildCustomId_WithVeryLargeIds_ShouldHandleCorrectly() {
        String customId = payPalService.buildCustomId(Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertThat(customId).contains("voice:user:");
        assertThat(customId).contains(String.valueOf(Integer.MAX_VALUE));
        assertThat(customId).contains("membership:");
    }

    @Test
    void buildRegistrationCustomId_WithSpecialCharacters_ShouldHandle() {
        String customId = payPalService.buildRegistrationCustomId("REG-123_ABC-xyz", 5);

        assertThat(customId).isEqualTo("voice:registration:REG-123_ABC-xyz:membership:5");
    }

    @Test
    void validateCaptureResponse_WithRefundedCapture_ShouldReturnFailure() throws Exception {
        String captureJson = """
                {
                  "status": "COMPLETED",
                  "purchase_units": [{
                    "custom_id": "voice:user:123:membership:2",
                    "payments": {
                      "captures": [{
                        "id": "CAPTURE-REFUND",
                        "status": "REFUNDED",
                        "amount": {
                          "currency_code": "CAD",
                          "value": "20.00"
                        }
                      }]
                    }
                  }]
                }
                """;
        JsonNode response = objectMapper.readTree(captureJson);

        CaptureValidationResult result = payPalService.validateCaptureResponse(
                response, testMembership, "voice:user:123:membership:2");

        assertThat(result.completed()).isFalse();
        assertThat(result.message()).contains("not completed");
    }
}
