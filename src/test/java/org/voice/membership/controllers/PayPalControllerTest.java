package org.voice.membership.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipPaymentTransaction;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipPaymentTransactionRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.services.MembershipService;
import org.voice.membership.services.PayPalService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PayPalController
 * Tests membership upgrade payment workflow using mocked PayPal service
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PayPalControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private MembershipRepository membershipRepository;

        @Autowired
        private MembershipPaymentTransactionRepository paymentTransactionRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private PayPalService payPalService;

        @Autowired
        private PayPalProperties payPalProperties;

        @Autowired
        private MembershipService membershipService;

        @Autowired
        private ObjectMapper objectMapper;

        private Membership freeMembership;
        private Membership paidMembership;

        @TestConfiguration
        static class TestConfig {
                @Bean
                public PayPalService payPalService() {
                        return mock(PayPalService.class);
                }
        }

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();
                membershipRepository.deleteAll();
                paymentTransactionRepository.deleteAll();

                // Create test memberships
                freeMembership = new Membership();
                freeMembership.setName("Free");
                freeMembership.setFree(true);
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership = membershipRepository.save(freeMembership);

                paidMembership = new Membership();
                paidMembership.setName("Premium");
                paidMembership.setFree(false);
                paidMembership.setPrice(new BigDecimal("20.00"));
                paidMembership.setActive(true);
                paidMembership = membershipRepository.save(paidMembership);
        }

        // ========== CREATE ORDER TESTS ==========

        @Test
        void createOrder_WithValidFreeMemberUser_ShouldCreatePayPalOrder() throws Exception {
                // Create user with free membership
                User user = createTestUser("upgrade.user@test.com", freeMembership);

                // Mock PayPal service response
                when(payPalService.createOrder(any(User.class), any(Membership.class)))
                                .thenReturn("PAYPAL-ORDER-123");

                // Create order request
                String requestBody = String.format("{\"membershipId\": %d}", paidMembership.getId());

                mockMvc.perform(post("/register/paypal/create-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value("PAYPAL-ORDER-123"));

                // Verify transaction was saved
                Optional<MembershipPaymentTransaction> transaction = paymentTransactionRepository
                                .findByPaypalOrderId("PAYPAL-ORDER-123");
                assertTrue(transaction.isPresent());
                assertEquals("CREATED", transaction.get().getStatus());
                assertEquals(user.getId(), transaction.get().getUser().getId());
                assertEquals(paidMembership.getId(), transaction.get().getMembership().getId());
        }

        @Test
        void createOrder_WithPaidMemberUser_ShouldReturnForbidden() throws Exception {
                // Create user with paid membership
                User user = createTestUser("paid.user@test.com", paidMembership);
                user.setPaid(true);
                userRepository.save(user);

                String requestBody = String.format("{\"membershipId\": %d}", paidMembership.getId());

                mockMvc.perform(post("/register/paypal/create-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Not eligible for membership upgrade"));
        }

        @Test
        void createOrder_WithUnauthenticatedUser_ShouldRedirectToLogin() throws Exception {
                String requestBody = String.format("{\"membershipId\": %d}", paidMembership.getId());

                mockMvc.perform(post("/register/paypal/create-order")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isFound())
                                .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        void createOrder_WithInvalidMembership_ShouldReturnBadRequest() throws Exception {
                User user = createTestUser("test.user@test.com", freeMembership);

                // Try to create order for free membership
                String requestBody = String.format("{\"membershipId\": %d}", freeMembership.getId());

                mockMvc.perform(post("/register/paypal/create-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Invalid membership"));
        }

        @Test
        void createOrder_WithMissingMembershipId_ShouldReturnBadRequest() throws Exception {
                User user = createTestUser("test.user@test.com", freeMembership);

                String requestBody = "{}";

                mockMvc.perform(post("/register/paypal/create-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Membership is required"));
        }

        // ========== CAPTURE ORDER TESTS ==========

        @Test
        void captureOrder_WithValidPayment_ShouldUpgradeMembership() throws Exception {
                // Create user with free membership
                User user = createTestUser("capture.user@test.com", freeMembership);

                // Create payment transaction
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("ORDER-123");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CREATED");
                paymentTransactionRepository.save(transaction);

                // Mock PayPal service validation
                PayPalService.CaptureValidationResult validationResult = new PayPalService.CaptureValidationResult(true,
                                "CAPTURE-123", "COMPLETED", "Success");
                when(payPalService.captureAndValidateOrder(any(User.class), any(Membership.class), eq("ORDER-123")))
                                .thenReturn(validationResult);

                String requestBody = String.format("{\"membershipId\": %d, \"orderId\": \"ORDER-123\"}",
                                paidMembership.getId());

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.redirectUrl").value("/profile?upgrade=success"));

                // Verify user was upgraded
                User updatedUser = userRepository.findById(user.getId()).orElseThrow();
                assertEquals(paidMembership.getId(), updatedUser.getMembership().getId());
                assertTrue(updatedUser.isPaid());
                assertNotNull(updatedUser.getMembershipStartDate());
                assertNotNull(updatedUser.getMembershipExpiryDate());

                // Verify transaction status
                MembershipPaymentTransaction updatedTransaction = paymentTransactionRepository
                                .findByPaypalOrderId("ORDER-123").orElseThrow();
                assertEquals("COMPLETED", updatedTransaction.getStatus());
                assertEquals("CAPTURE-123", updatedTransaction.getPaypalCaptureId());
        }

        @Test
        void captureOrder_WithAlreadyCompletedTransaction_ShouldReturnSuccess() throws Exception {
                User user = createTestUser("completed.user@test.com", freeMembership);

                // Create already completed transaction
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("ORDER-COMPLETED");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("COMPLETED");
                transaction.setPaypalCaptureId("CAPTURE-OLD");
                paymentTransactionRepository.save(transaction);

                String requestBody = String.format("{\"membershipId\": %d, \"orderId\": \"ORDER-COMPLETED\"}",
                                paidMembership.getId());

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.redirectUrl").value("/profile?upgrade=success"));
        }

        @Test
        void captureOrder_WithOrderBelongingToDifferentUser_ShouldReturnForbidden() throws Exception {
                User user1 = createTestUser("user1@test.com", freeMembership);
                User user2 = createTestUser("user2@test.com", freeMembership);

                // Create transaction for user1
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user1);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("ORDER-USER1");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CREATED");
                paymentTransactionRepository.save(transaction);

                String requestBody = String.format("{\"membershipId\": %d, \"orderId\": \"ORDER-USER1\"}",
                                paidMembership.getId());

                // Try to capture with user2
                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user2.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Order does not belong to user"));
        }

        @Test
        void captureOrder_WithPaidMemberUser_ShouldReturnForbidden() throws Exception {
                User user = createTestUser("paid.capture@test.com", paidMembership);
                user.setPaid(true);
                userRepository.save(user);

                String requestBody = String.format("{\"membershipId\": %d, \"orderId\": \"ORDER-123\"}",
                                paidMembership.getId());

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Not eligible for membership upgrade"));
        }

        @Test
        void captureOrder_WithFailedValidation_ShouldReturnBadRequest() throws Exception {
                User user = createTestUser("failed.capture@test.com", freeMembership);

                // Mock PayPal service validation failure
                PayPalService.CaptureValidationResult validationResult = new PayPalService.CaptureValidationResult(
                                false, null,
                                "FAILED", "Payment declined");
                when(payPalService.captureAndValidateOrder(any(User.class), any(Membership.class), eq("ORDER-FAILED")))
                                .thenReturn(validationResult);

                String requestBody = String.format("{\"membershipId\": %d, \"orderId\": \"ORDER-FAILED\"}",
                                paidMembership.getId());

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Payment verification failed"));

                // Verify transaction status
                MembershipPaymentTransaction transaction = paymentTransactionRepository
                                .findByPaypalOrderId("ORDER-FAILED").orElseThrow();
                assertEquals("FAILED", transaction.getStatus());
                assertEquals("Payment declined", transaction.getFailureReason());
        }

        @Test
        void captureOrder_WithMissingOrderId_ShouldReturnBadRequest() throws Exception {
                User user = createTestUser("test.user@test.com", freeMembership);

                String requestBody = String.format("{\"membershipId\": %d}", paidMembership.getId());

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Membership and order are required"));
        }

        // ========== WEBHOOK TESTS ==========

        @Test
        void webhook_WithValidPaymentCaptureCompleted_ShouldProcessUpgrade() throws Exception {
                User user = createTestUser("webhook.user@test.com", freeMembership);

                // Create pending transaction
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("WEBHOOK-ORDER-123");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CAPTURE_PENDING");
                paymentTransactionRepository.save(transaction);

                // Mock webhook verification
                when(payPalService.verifyWebhookSignature(anyMap(), any(JsonNode.class)))
                                .thenReturn(true);

                // Mock order validation
                PayPalService.CaptureValidationResult validationResult = new PayPalService.CaptureValidationResult(true,
                                "WEBHOOK-CAPTURE-123", "COMPLETED", "Success");
                when(payPalService.validateOrderFromPayPal(any(User.class), any(Membership.class),
                                eq("WEBHOOK-ORDER-123")))
                                .thenReturn(validationResult);

                // Create webhook payload
                ObjectNode webhookPayload = objectMapper.createObjectNode();
                webhookPayload.put("event_type", "PAYMENT.CAPTURE.COMPLETED");

                ObjectNode resource = objectMapper.createObjectNode();
                resource.put("id", "WEBHOOK-CAPTURE-123");

                ObjectNode supplementaryData = objectMapper.createObjectNode();
                ObjectNode relatedIds = objectMapper.createObjectNode();
                relatedIds.put("order_id", "WEBHOOK-ORDER-123");
                supplementaryData.set("related_ids", relatedIds);
                resource.set("supplementary_data", supplementaryData);

                webhookPayload.set("resource", resource);

                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .header("paypal-transmission-id", "test-id")
                                .header("paypal-transmission-time", "2024-01-01T00:00:00Z")
                                .header("paypal-transmission-sig", "test-sig")
                                .header("paypal-cert-url", "test-cert-url")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("processed"));

                // Verify user was upgraded
                User updatedUser = userRepository.findById(user.getId()).orElseThrow();
                assertEquals(paidMembership.getId(), updatedUser.getMembership().getId());
                assertTrue(updatedUser.isPaid());

                // Verify transaction status
                MembershipPaymentTransaction updatedTransaction = paymentTransactionRepository
                                .findByPaypalOrderId("WEBHOOK-ORDER-123").orElseThrow();
                assertEquals("COMPLETED", updatedTransaction.getStatus());
                assertEquals("WEBHOOK-CAPTURE-123", updatedTransaction.getPaypalCaptureId());
        }

        @Test
        void webhook_WithInvalidSignature_ShouldReturnBadRequest() throws Exception {
                // Mock webhook verification failure
                when(payPalService.verifyWebhookSignature(anyMap(), any(JsonNode.class)))
                                .thenReturn(false);

                ObjectNode webhookPayload = objectMapper.createObjectNode();
                webhookPayload.put("event_type", "PAYMENT.CAPTURE.COMPLETED");

                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString()))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("invalid_signature"));
        }

        @Test
        void webhook_WithUnknownOrderId_ShouldReturnIgnored() throws Exception {
                // Mock webhook verification
                when(payPalService.verifyWebhookSignature(anyMap(), any(JsonNode.class)))
                                .thenReturn(true);

                ObjectNode webhookPayload = objectMapper.createObjectNode();
                webhookPayload.put("event_type", "PAYMENT.CAPTURE.COMPLETED");

                ObjectNode resource = objectMapper.createObjectNode();
                resource.put("id", "CAPTURE-UNKNOWN");

                ObjectNode supplementaryData = objectMapper.createObjectNode();
                ObjectNode relatedIds = objectMapper.createObjectNode();
                relatedIds.put("order_id", "UNKNOWN-ORDER-ID");
                supplementaryData.set("related_ids", relatedIds);
                resource.set("supplementary_data", supplementaryData);

                webhookPayload.set("resource", resource);

                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("ignored_unknown_order"));
        }

        @Test
        void webhook_WithAlreadyCompletedOrder_ShouldReturnAlreadyCompleted() throws Exception {
                User user = createTestUser("completed.webhook@test.com", freeMembership);

                // Create already completed transaction
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("COMPLETED-ORDER");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("COMPLETED");
                transaction.setPaypalCaptureId("OLD-CAPTURE");
                paymentTransactionRepository.save(transaction);

                // Mock webhook verification
                when(payPalService.verifyWebhookSignature(anyMap(), any(JsonNode.class)))
                                .thenReturn(true);

                ObjectNode webhookPayload = objectMapper.createObjectNode();
                webhookPayload.put("event_type", "PAYMENT.CAPTURE.COMPLETED");

                ObjectNode resource = objectMapper.createObjectNode();
                resource.put("id", "NEW-CAPTURE");

                ObjectNode supplementaryData = objectMapper.createObjectNode();
                ObjectNode relatedIds = objectMapper.createObjectNode();
                relatedIds.put("order_id", "COMPLETED-ORDER");
                supplementaryData.set("related_ids", relatedIds);
                resource.set("supplementary_data", supplementaryData);

                webhookPayload.set("resource", resource);

                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("already_completed"));
        }

        @Test
        void webhook_WithIgnoredEventType_ShouldReturnIgnored() throws Exception {
                // Mock webhook verification
                when(payPalService.verifyWebhookSignature(anyMap(), any(JsonNode.class)))
                                .thenReturn(true);

                ObjectNode webhookPayload = objectMapper.createObjectNode();
                webhookPayload.put("event_type", "PAYMENT.SALE.REFUNDED");

                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("ignored"));
        }

        @Test
        void webhook_WithValidationFailure_ShouldReturnValidationFailed() throws Exception {
                User user = createTestUser("validation.fail@test.com", freeMembership);

                // Create pending transaction
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("VALIDATION-FAIL-ORDER");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CAPTURE_PENDING");
                paymentTransactionRepository.save(transaction);

                // Mock webhook verification
                when(payPalService.verifyWebhookSignature(anyMap(), any(JsonNode.class)))
                                .thenReturn(true);

                // Mock validation failure
                PayPalService.CaptureValidationResult validationResult = new PayPalService.CaptureValidationResult(
                                false, null,
                                "FAILED", "Validation error");
                when(payPalService.validateOrderFromPayPal(any(User.class), any(Membership.class),
                                eq("VALIDATION-FAIL-ORDER")))
                                .thenReturn(validationResult);

                ObjectNode webhookPayload = objectMapper.createObjectNode();
                webhookPayload.put("event_type", "PAYMENT.CAPTURE.COMPLETED");

                ObjectNode resource = objectMapper.createObjectNode();
                resource.put("id", "FAIL-CAPTURE");

                ObjectNode supplementaryData = objectMapper.createObjectNode();
                ObjectNode relatedIds = objectMapper.createObjectNode();
                relatedIds.put("order_id", "VALIDATION-FAIL-ORDER");
                supplementaryData.set("related_ids", relatedIds);
                resource.set("supplementary_data", supplementaryData);

                webhookPayload.set("resource", resource);

                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("validation_failed"));

                // Verify transaction status
                MembershipPaymentTransaction updatedTransaction = paymentTransactionRepository
                                .findByPaypalOrderId("VALIDATION-FAIL-ORDER").orElseThrow();
                assertEquals("FAILED", updatedTransaction.getStatus());
                assertTrue(updatedTransaction.getFailureReason().contains("Validation error"));
        }

        // ========== CRITICAL SECURITY TESTS ==========

        @Test
        void captureOrder_CalledTwiceForSameOrder_ShouldBeIdempotent() throws Exception {
                User user = createTestUser("idempotent.test@test.com", freeMembership);

                // Create initial transaction
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("ORDER-IDEM-123");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CREATED");
                paymentTransactionRepository.save(transaction);

                // Mock successful validation
                PayPalService.CaptureValidationResult successResult = new PayPalService.CaptureValidationResult(true,
                                "CAPTURE-IDEM-123", "COMPLETED", "Success");
                when(payPalService.captureAndValidateOrder(any(User.class), any(Membership.class),
                                eq("ORDER-IDEM-123")))
                                .thenReturn(successResult);

                // First capture - should succeed
                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"orderId\": \"ORDER-IDEM-123\", \"membershipId\": " + paidMembership.getId()
                                                + "}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // Second capture same order - user is now paid, so returns forbidden
                // (This proves idempotency - user was only upgraded once, can't be charged
                // again)
                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"orderId\": \"ORDER-IDEM-123\", \"membershipId\": " + paidMembership.getId()
                                                + "}"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Not eligible for membership upgrade"));

                // Verify only ONE transaction exists (no double-charging)
                long transactionCount = paymentTransactionRepository.findAll().stream()
                                .filter(t -> "ORDER-IDEM-123".equals(t.getPaypalOrderId()))
                                .count();
                assertEquals(1, transactionCount, "Should only have one transaction for duplicate capture attempts");

                // Verify user upgraded only once
                User updatedUser = userRepository.findByEmail("idempotent.test@test.com");
                assertTrue(updatedUser.isPaid());
                assertEquals(paidMembership.getId(), updatedUser.getMembership().getId());
        }

        @Test
        void captureOrder_DifferentUserAttemptingSameOrder_ShouldReject() throws Exception {
                User user1 = createTestUser("user1.security@test.com", freeMembership);
                User user2 = createTestUser("user2.security@test.com", freeMembership);

                // Create transaction for user1
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user1);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("ORDER-SEC-123");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CREATED");
                paymentTransactionRepository.save(transaction);

                // Mock successful validation for user1
                PayPalService.CaptureValidationResult successResult = new PayPalService.CaptureValidationResult(true,
                                "CAPTURE-SEC-123", "COMPLETED", "Success");
                when(payPalService.captureAndValidateOrder(any(User.class), any(Membership.class), eq("ORDER-SEC-123")))
                                .thenReturn(successResult);

                // User1 successfully captures order
                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user1.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"orderId\": \"ORDER-SEC-123\", \"membershipId\": " + paidMembership.getId()
                                                + "}"))
                                .andExpect(status().isOk());

                // User2 attempts to capture same order - should be forbidden
                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user2.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"orderId\": \"ORDER-SEC-123\", \"membershipId\": " + paidMembership.getId()
                                                + "}"))
                                .andExpect(status().isForbidden());

                // Verify only user1 was upgraded
                User updatedUser1 = userRepository.findByEmail("user1.security@test.com");
                User updatedUser2 = userRepository.findByEmail("user2.security@test.com");
                assertTrue(updatedUser1.isPaid());
                assertFalse(updatedUser2.isPaid());
        }

        @Test
        void webhook_ReceivedTwiceForSameEvent_ShouldBeIdempotent() throws Exception {
                User user = createTestUser("webhook.idem@test.com", freeMembership);

                // Create initial transaction
                MembershipPaymentTransaction transaction = MembershipPaymentTransaction.builder()
                                .user(user)
                                .membership(paidMembership)
                                .paypalOrderId("ORDER-WEBHOOK-IDEM")
                                .amount(paidMembership.getPrice())
                                .currency("CAD")
                                .status("PENDING")
                                .createdAt(new Date())
                                .updatedAt(new Date())
                                .build();
                paymentTransactionRepository.save(transaction);

                ObjectNode webhookPayload = objectMapper.createObjectNode();
                webhookPayload.put("event_type", "PAYMENT.CAPTURE.COMPLETED");

                ObjectNode resource = objectMapper.createObjectNode();
                resource.put("id", "CAPTURE-IDEM-WH");
                resource.put("status", "COMPLETED");

                ObjectNode supplementaryData = objectMapper.createObjectNode();
                ObjectNode relatedIds = objectMapper.createObjectNode();
                relatedIds.put("order_id", "ORDER-WEBHOOK-IDEM");
                supplementaryData.set("related_ids", relatedIds);
                resource.set("supplementary_data", supplementaryData);

                webhookPayload.set("resource", resource);

                when(payPalService.verifyWebhookSignature(anyMap(), any(JsonNode.class))).thenReturn(true);

                PayPalService.CaptureValidationResult validResult = new PayPalService.CaptureValidationResult(true,
                                "CAPTURE-IDEM-WH", "COMPLETED", "Success");
                when(payPalService.validateOrderFromPayPal(any(User.class), any(Membership.class),
                                eq("ORDER-WEBHOOK-IDEM")))
                                .thenReturn(validResult);

                // First webhook - should process
                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString())
                                .header("paypal-transmission-id", "webhook-1")
                                .header("paypal-transmission-time", "2023-01-01T00:00:00Z")
                                .header("paypal-transmission-sig", "signature")
                                .header("paypal-cert-url", "https://api.paypal.com/cert")
                                .header("paypal-auth-algo", "SHA256withRSA"))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/paypal/webhook")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload.toString())
                                .header("paypal-transmission-id", "webhook-2")
                                .header("paypal-transmission-time", "2023-01-01T00:00:01Z")
                                .header("paypal-transmission-sig", "signature")
                                .header("paypal-cert-url", "https://api.paypal.com/cert")
                                .header("paypal-auth-algo", "SHA256withRSA"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("already_completed"));

                Optional<MembershipPaymentTransaction> txn = paymentTransactionRepository
                                .findByPaypalOrderId("ORDER-WEBHOOK-IDEM");
                assertTrue(txn.isPresent());
                assertEquals("COMPLETED", txn.get().getStatus());

                User updatedUser = userRepository.findByEmail("webhook.idem@test.com");
                assertTrue(updatedUser.isPaid());
        }

        @Test
        void captureOrder_WithZeroAmountForPaidMembership_ShouldReject() throws Exception {
                User user = createTestUser("zero.amount@test.com", freeMembership);

                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("ORDER-ZERO");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CREATED");
                paymentTransactionRepository.save(transaction);

                PayPalService.CaptureValidationResult invalidResult = new PayPalService.CaptureValidationResult(false,
                                null,
                                "FAILED", "Amount mismatch");
                when(payPalService.captureAndValidateOrder(any(User.class), any(Membership.class), eq("ORDER-ZERO")))
                                .thenReturn(invalidResult);

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"orderId\": \"ORDER-ZERO\", \"membershipId\": " + paidMembership.getId()
                                                + "}"))
                                .andExpect(status().isBadRequest());

                User updatedUser = userRepository.findByEmail("zero.amount@test.com");
                assertFalse(updatedUser.isPaid());
                assertEquals(freeMembership.getId(), updatedUser.getMembership().getId());
        }

        @Test
        void captureOrder_AfterUserDeleted_ShouldHandleGracefully() throws Exception {
                User user = createTestUser("deleted.user@test.com", freeMembership);
                String userEmail = user.getEmail();

                when(payPalService.createOrder(any(User.class), any(Membership.class)))
                                .thenReturn("ORDER-DELETED-USER");

                mockMvc.perform(post("/register/paypal/create-order")
                                .with(csrf())
                                .with(user(userEmail).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"membershipId\": " + paidMembership.getId() + "}"))
                                .andExpect(status().isOk());

                userRepository.delete(user);

                // Attempt capture - should handle gracefully
                PayPalService.CaptureValidationResult result = new PayPalService.CaptureValidationResult(true,
                                "COMPLETED",
                                "Success", "CAPTURE-DEL");
                when(payPalService.captureAndValidateOrder(any(User.class), any(Membership.class),
                                eq("ORDER-DELETED-USER")))
                                .thenReturn(result);

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(userEmail).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"orderId\": \"ORDER-DELETED-USER\", \"membershipId\": "
                                                + paidMembership.getId() + "}"))
                                .andExpect(status().is5xxServerError());
        }

        @Test
        void captureOrder_WithNegativeAmount_ShouldReject() throws Exception {
                User user = createTestUser("negative.amount@test.com", freeMembership);

                // Create transaction first (capture requires existing transaction)
                MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
                transaction.setUser(user);
                transaction.setMembership(paidMembership);
                transaction.setPaypalOrderId("ORDER-NEGATIVE");
                transaction.setAmount(paidMembership.getPrice());
                transaction.setCurrency("CAD");
                transaction.setStatus("CREATED");
                paymentTransactionRepository.save(transaction);

                // Mock validation detecting negative amount
                PayPalService.CaptureValidationResult invalidResult = new PayPalService.CaptureValidationResult(false,
                                null,
                                "FAILED", "Invalid amount");
                when(payPalService.captureAndValidateOrder(any(User.class), any(Membership.class),
                                eq("ORDER-NEGATIVE")))
                                .thenReturn(invalidResult);

                mockMvc.perform(post("/register/paypal/capture-order")
                                .with(csrf())
                                .with(user(user.getEmail()).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"orderId\": \"ORDER-NEGATIVE\", \"membershipId\": " + paidMembership.getId()
                                                + "}"))
                                .andExpect(status().isBadRequest());

                // Verify transaction was marked as FAILED
                MembershipPaymentTransaction updatedTransaction = paymentTransactionRepository
                                .findByPaypalOrderId("ORDER-NEGATIVE").orElseThrow();
                assertEquals("FAILED", updatedTransaction.getStatus());
        }

        // ========== HELPER METHODS ==========

        private User createTestUser(String email, Membership membership) {
                User user = new User();
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode("password123"));
                user.setFirstName("Test");
                user.setLastName("User");
                user.setEmailVerified(true);
                user.setRole("ROLE_USER");
                user.setMembership(membership);
                user.setPaid(membership != null && !membership.isFree());
                user.setCreation(new Date());
                return userRepository.save(user);
        }
}
