package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipPaymentTransaction;
import org.voice.membership.entities.User;
import org.voice.membership.entities.VerificationToken;
import org.voice.membership.repositories.MembershipPaymentTransactionRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.VerificationTokenRepository;
import org.voice.membership.services.PayPalService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for complete user registration workflow
 * Tests the entire registration journey from form submission to account
 * activation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationWorkflowIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public PayPalService payPalService() {
            return mock(PayPalService.class);
        }
        
        @Bean
        @Primary
        public JavaMailSender javaMailSender() {
            JavaMailSender mailSender = mock(JavaMailSender.class);
            when(mailSender.createMimeMessage()).thenAnswer(invocation -> {
                jakarta.mail.Session session = jakarta.mail.Session.getInstance(System.getProperties());
                return new jakarta.mail.internet.MimeMessage(session);
            });
            return mailSender;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private MembershipPaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PayPalService payPalService;

    private Membership freeMembership;
    private Membership paidMembership;

    @BeforeEach
    void setUp() {
        // Reset mock before each test
        reset(payPalService);
        
        userRepository.deleteAll();
        membershipRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        paymentTransactionRepository.deleteAll();

        // Create free membership
        freeMembership = new Membership();
        freeMembership.setName("Free Membership");
        freeMembership.setPrice(BigDecimal.ZERO);
        freeMembership.setActive(true);
        freeMembership.setFree(true);
        freeMembership.setDisplayOrder(1);
        freeMembership = membershipRepository.save(freeMembership);

        // Create paid membership
        paidMembership = new Membership();
        paidMembership.setName("Premium Membership");
        paidMembership.setPrice(new BigDecimal("20.00"));
        paidMembership.setActive(true);
        paidMembership.setFree(false);
        paidMembership.setDisplayOrder(2);
        paidMembership = membershipRepository.save(paidMembership);
    }

    // ==================== Complete Registration Workflow Tests
    // ====================

    @Test
    void testCompleteRegistrationWorkflowWithFreeMembership() throws Exception {
        // Step 1: Access registration page
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerDto"));

        // Step 2: Submit step 1 (user details)
        MvcResult result = mockMvc.perform(post("/register/step1")
                .with(csrf())
                .param("firstName", "John")
                .param("middleName", "")
                .param("lastName", "Doe")
                .param("email", "john.workflow@example.com")
                .param("password", "SecurePass123!")
                .param("confirmPassword", "SecurePass123!")
                .param("phone", "1234567890")
                .param("address", "123 Main St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M5H 2N2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/step2"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

        // Step 3: Submit step 2 (child details)
        mockMvc.perform(post("/register/step2")
                .session(session)
                .with(csrf())
                .param("childName", "Jane Doe")
                .param("childAge", "8"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/step3"));

        // Step 4: Access step 3 (membership selection)
        mockMvc.perform(get("/register/step3")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("register-step3"))
                .andExpect(model().attributeExists("memberships"));

        // Step 5: Select free membership and complete registration
        mockMvc.perform(post("/register/step3")
                .session(session)
                .with(csrf())
                .param("membershipId", String.valueOf(freeMembership.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/step4"));

        // Step 6: Submit step 4 to complete registration
        mockMvc.perform(post("/register/step4")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/verification-sent"));

        // Verify user was created
        User createdUser = userRepository.findByEmail("john.workflow@example.com");
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getFirstName()).isEqualTo("John");
        assertThat(createdUser.getLastName()).isEqualTo("Doe");
        assertThat(createdUser.isEmailVerified()).isFalse();

        // Verify verification token was created
        VerificationToken token = verificationTokenRepository.findByUser(createdUser).orElse(null);
        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotEmpty();
    }

    @Test
    void testCompleteRegistrationWorkflowWithPaidMembershipAndPayPalPayment() throws Exception {
        // Mock PayPal service responses for registration flow
        String testOrderId = "PAYPAL-ORDER-TEST-123";
        when(payPalService.createOrderForRegistration(any(Membership.class), anyString()))
                .thenReturn(testOrderId);
        
        PayPalService.CaptureValidationResult successResult = new PayPalService.CaptureValidationResult(
                true, "CAPTURE-TEST-123", "COMPLETED", "Payment successful");
        when(payPalService.captureAndValidateRegistration(any(Membership.class), eq(testOrderId), anyString()))
                .thenReturn(successResult);

        // Step 1: Submit user details
        MvcResult result = mockMvc.perform(post("/register/step1")
                .with(csrf())
                .param("firstName", "Premium")
                .param("middleName", "")
                .param("lastName", "User")
                .param("email", "premium.user@example.com")
                .param("password", "SecurePass123!")
                .param("confirmPassword", "SecurePass123!")
                .param("phone", "4165551234")
                .param("address", "789 Premium Ave")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M5H 2N2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/step2"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

        // Step 2: Add child information
        mockMvc.perform(post("/register/step2")
                .session(session)
                .with(csrf())
                .param("childName", "Premium Kid")
                .param("childAge", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/step3"));

        // Step 3: Select PAID membership
        mockMvc.perform(post("/register/step3")
                .session(session)
                .with(csrf())
                .param("membershipId", String.valueOf(paidMembership.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/step4"));

        // Step 4: View checkout page
        mockMvc.perform(get("/register/checkout")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attributeExists("membership"));

        // Step 5: Create PayPal order via API (registration checkout endpoint)
        MvcResult orderResult = mockMvc.perform(post("/register/paypal/checkout/create-order")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipId\": " + paidMembership.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();

        String orderResponse = orderResult.getResponse().getContentAsString();
        assertThat(orderResponse).contains(testOrderId);

        // Verify order ID was stored in session
        String sessionOrderId = (String) session.getAttribute("registrationPayPalOrderId");
        assertThat(sessionOrderId).isEqualTo(testOrderId);

        // Step 6: Capture PayPal payment (registration checkout endpoint)
        MvcResult captureResult = mockMvc.perform(post("/register/paypal/checkout/capture-order")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipId\": " + paidMembership.getId() + ", \"orderId\": \"" + testOrderId + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String captureResponse = captureResult.getResponse().getContentAsString();
        assertThat(captureResponse).contains("success");
        assertThat(captureResponse).contains("redirectUrl");

        // Verify user was created with paid membership
        User createdUser = userRepository.findByEmail("premium.user@example.com");
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getFirstName()).isEqualTo("Premium");
        assertThat(createdUser.getLastName()).isEqualTo("User");
        assertThat(createdUser.getMembership().getId()).isEqualTo(paidMembership.getId());
        assertThat(createdUser.isPaid()).isTrue();
        assertThat(createdUser.getMembershipStartDate()).isNotNull();
        assertThat(createdUser.getMembershipExpiryDate()).isNotNull();
        assertThat(createdUser.isEmailVerified()).isFalse();

        // Verify payment transaction was completed
        MembershipPaymentTransaction completedTransaction = paymentTransactionRepository
                .findByPaypalOrderId(testOrderId).orElseThrow();
        assertThat(completedTransaction.getStatus()).isEqualTo("COMPLETED");
        assertThat(completedTransaction.getPaypalCaptureId()).isEqualTo("CAPTURE-TEST-123");
        assertThat(completedTransaction.getUser().getId()).isEqualTo(createdUser.getId());

        // Verify verification token was created
        VerificationToken token = verificationTokenRepository.findByUser(createdUser).orElse(null);
        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotEmpty();

        // Step 7: Complete email verification
        mockMvc.perform(get("/register/verify")
                .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-result"))
                .andExpect(model().attribute("success", "Email verified successfully! You can now login to your account."));

        // Verify user is now email verified
        User verifiedUser = userRepository.findByEmail("premium.user@example.com");
        assertThat(verifiedUser.isEmailVerified()).isTrue();
        assertThat(verifiedUser.isPaid()).isTrue();
        assertThat(verifiedUser.getMembership().getName()).isEqualTo("Premium Membership");
    }

    @Test
    void testEmailVerificationWorkflow() throws Exception {
        // Create a test user
        User user = new User();
        user.setFirstName("Verify");
        user.setLastName("User");
        user.setEmail("verify.workflow@example.com");
        user.setPassword("encodedPassword");
        user.setPhone("1234567890");
        user.setAddress("123 Test St");
        user.setCity("Toronto");
        user.setProvince("ON");
        user.setPostalCode("M5H 2N2");
        user.setRole("USER");
        user.setEmailVerified(false);
        user = userRepository.save(user);

        // Create verification token
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken(tokenValue, user);
        verificationTokenRepository.save(token);

        // Verify email with token
        mockMvc.perform(get("/register/verify")
                .param("token", tokenValue))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-result"))
                .andExpect(model().attribute("success",
                        "Email verified successfully! You can now login to your account."));

        // Verify user is now verified
        User verifiedUser = userRepository.findByEmail("verify.workflow@example.com");
        assertThat(verifiedUser.isEmailVerified()).isTrue();

        // Verify token is deleted after use
        VerificationToken usedToken = verificationTokenRepository.findByToken(tokenValue).orElse(null);
        assertThat(usedToken).isNull();
    }

    @Test
    void testRegistrationSessionExpiration() throws Exception {
        // Try to access step 2 without session
        mockMvc.perform(get("/register/step2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        // Try to access step 3 without session
        mockMvc.perform(get("/register/step3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        // Try to submit step 2 without session
        mockMvc.perform(post("/register/step2")
                .with(csrf())
                .param("childName", "Test Child")
                .param("childAge", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    @Test
    void testRegistrationWithDuplicateEmail() throws Exception {
        // Create existing user
        User existingUser = new User();
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        existingUser.setEmail("duplicate@example.com");
        existingUser.setPassword("password");
        existingUser.setPhone("1234567890");
        existingUser.setAddress("123 Test St");
        existingUser.setCity("Toronto");
        existingUser.setProvince("ON");
        existingUser.setPostalCode("M5H 2N2");
        existingUser.setRole("USER");
        userRepository.save(existingUser);

        // Try to register with duplicate email
        mockMvc.perform(post("/register/step1")
                .with(csrf())
                .param("firstName", "New")
                .param("lastName", "User")
                .param("email", "duplicate@example.com")
                .param("password", "SecurePass123!")
                .param("confirmPassword", "SecurePass123!")
                .param("phone", "9876543210")
                .param("address", "456 Other St")
                .param("city", "Vancouver")
                .param("province", "BC")
                .param("postalCode", "V6B 1A1"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registerDto", "email"));
    }

    @Test
    void testRegistrationWithInvalidPasswordConfirmation() throws Exception {
        mockMvc.perform(post("/register/step1")
                .with(csrf())
                .param("firstName", "Test")
                .param("lastName", "User")
                .param("email", "test@example.com")
                .param("password", "SecurePass123!")
                .param("confirmPassword", "DifferentPass456!")
                .param("phone", "1234567890")
                .param("address", "123 Test St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M5H 2N2"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testVerificationWithInvalidToken() throws Exception {
        mockMvc.perform(get("/register/verify")
                .param("token", "invalid-token-12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-result"))
                .andExpect(model().attribute("error", "Invalid verification token."));
    }

    @Test
    void testVerificationWithExpiredToken() throws Exception {
        // Create a test user
        User user = new User();
        user.setFirstName("Expired");
        user.setLastName("User");
        user.setEmail("expired.token@example.com");
        user.setPassword("encodedPassword");
        user.setPhone("1234567890");
        user.setAddress("123 Test St");
        user.setCity("Toronto");
        user.setProvince("ON");
        user.setPostalCode("M5H 2N2");
        user.setRole("USER");
        user.setEmailVerified(false);
        user = userRepository.save(user);

        // Create expired verification token
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setToken(tokenValue);
        token.setUser(user);
        // Set expiry date to past (24 hours ago)
        token.setExpiryDate(new java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        verificationTokenRepository.save(token);

        // Try to verify with expired token
        mockMvc.perform(get("/register/verify")
                .param("token", tokenValue))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-result"))
                .andExpect(model().attribute("error", "Verification token has expired. Please register again."));

        // Verify user is still not verified
        User stillUnverified = userRepository.findByEmail("expired.token@example.com");
        assertThat(stillUnverified.isEmailVerified()).isFalse();
    }

    @Test
    void testRegistrationStepOrderEnforcement() throws Exception {
        // Try to skip to step 3 directly
        mockMvc.perform(get("/register/step3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        // Complete step 1
        MvcResult result = mockMvc.perform(post("/register/step1")
                .with(csrf())
                .param("firstName", "Step")
                .param("lastName", "Order")
                .param("email", "step.order@example.com")
                .param("password", "SecurePass123!")
                .param("confirmPassword", "SecurePass123!")
                .param("phone", "1234567890")
                .param("address", "123 Test St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M5H 2N2"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

        // Try to skip step 2 and go to step 3
        // Note: Step 3 is accessible after step 1 - it only checks if registration
        // session exists
        mockMvc.perform(get("/register/step3")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("register-step3"));
    }

    @Test
    void testResendVerificationEmail() throws Exception {
        // Create unverified user
        User user = new User();
        user.setFirstName("Resend");
        user.setLastName("User");
        user.setEmail("resend@example.com");
        user.setPassword("encodedPassword");
        user.setPhone("1234567890");
        user.setAddress("123 Test St");
        user.setCity("Toronto");
        user.setProvince("ON");
        user.setPostalCode("M5H 2N2");
        user.setRole("USER");
        user.setEmailVerified(false);
        userRepository.save(user);

        // Request resend verification email
        mockMvc.perform(post("/register/resend-verification")
                .with(csrf())
                .param("email", "resend@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("resend-verification"))
                .andExpect(model().attribute("success", "Verification email sent! Please check your inbox."));

        // Verify new token was created
        VerificationToken newToken = verificationTokenRepository.findByUser(user).orElse(null);
        assertThat(newToken).isNotNull();
    }
}
