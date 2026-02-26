package org.voice.membership.controllers;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.entities.VerificationToken;
import org.voice.membership.repositories.*;
import org.voice.membership.services.PayPalService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Optional;

import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for RegisterController
 * Tests user registration workflow using real services
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegisterControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private MembershipRepository membershipRepository;

        @Autowired
        private VerificationTokenRepository verificationTokenRepository;

        @Autowired
        private PayPalService payPalService;

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
        }

        // ======================Positive Tests======================
        // Show Registration Page
        @Test
        void showRegister_ShouldReturnRegistrationPage() throws Exception {
                mockMvc.perform(get("/register"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register"))
                                .andExpect(model().attributeExists("registerDto"));
        }

        // Valid Registration Data
        @Test
        void handleStep1_WithValidData_ShouldProceedToStep2() throws Exception {
                mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "John")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "john@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "123 Main St")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "A1A 1A1"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step2"));
        }

        // Show Step 2 With Valid Session Data
        @Test
        void showStep2_WithValidSessionData_ShouldDisplayChildForm() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(get("/register/step2")
                                .session(session))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register-step2"))
                                .andExpect(model().attributeExists("children"))
                                .andExpect(model().attribute("step", 2))
                                .andExpect(model().attribute("totalSteps", 4));
        }

        // Submit Step 2 With Valid Child Data
        @Test
        void handleStep2_WithValidChildData_ShouldProceedToStep3() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5")
                                .param("childDob", "2021-01-15")
                                .param("hearingLossType", "Bilateral")
                                .param("equipmentType", "Hearing Aid")
                                .param("siblingsNames", "None")
                                .param("chapterLocation", "Toronto"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step3"));
        }

        // Submit Step 2 With Multiple Children
        @Test
        void handleStep2_WithMultipleChildren_ShouldProceedToStep3() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe", "Sally Doe")
                                .param("childAge", "5", "7")
                                .param("childDob", "2021-01-15", "2019-03-20")
                                .param("hearingLossType", "Bilateral", "Unilateral")
                                .param("equipmentType", "Hearing Aid", "Cochlear Implant")
                                .param("siblingsNames", "Sally", "Tommy")
                                .param("chapterLocation", "Toronto", "Toronto"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step3"));
        }

        // Submit Step 2 Without Session Data
        @Test
        void handleStep2_WithoutSessionData_ShouldRedirectToStep1() throws Exception {
                mockMvc.perform(post("/register/step2")
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register"));
        }

        // Show Step 3 With Valid Session Data
        @Test
        void showStep3_WithValidSessionData_ShouldDisplayMembershipOptions() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership.setFree(true);
                freeMembership.setDisplayOrder(1);
                membershipRepository.save(freeMembership);

                MvcResult result1 = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result1.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(get("/register/step3")
                                .session(session))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register-step3"))
                                .andExpect(model().attributeExists("memberships"))
                                .andExpect(model().attribute("step", 3))
                                .andExpect(model().attribute("totalSteps", 4));
        }

        // Submit Step 3 With Valid Membership Selection
        @Test
        void handleStep3_WithValidMembershipSelection_ShouldProceedToStep4() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership.setFree(true);
                freeMembership.setDisplayOrder(1);
                Membership savedMembership = membershipRepository.save(freeMembership);

                MvcResult result1 = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result1.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step4"));
        }

        // Show Step 4 With Valid Session Data
        @Test
        void showStep4_WithValidSessionData_ShouldDisplayCart() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership.setFree(true);
                freeMembership.setDisplayOrder(1);
                Membership savedMembership = membershipRepository.save(freeMembership);

                MvcResult result1 = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result1.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                mockMvc.perform(get("/register/step4")
                                .session(session))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register-step4"))
                                .andExpect(model().attributeExists("membership"))
                                .andExpect(model().attribute("step", 4))
                                .andExpect(model().attribute("totalSteps", 4));
        }

        // Handle Step 4 - Remove from Cart
        @Test
        void handleStep4_WithRemoveAction_ShouldRedirectToStep3() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership.setFree(true);
                freeMembership.setDisplayOrder(1);
                Membership savedMembership = membershipRepository.save(freeMembership);

                MvcResult result1 = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave").param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result1.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                mockMvc.perform(post("/register/step4")
                                .session(session)
                                .with(csrf())
                                .param("action", "remove"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step3"));
        }

        // Handle Step 4 - Complete Registration with Free Membership
        @Test
        void handleStep4_WithFreeMembership_ShouldCompleteRegistration() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership.setFree(true);
                freeMembership.setDisplayOrder(1);
                Membership savedMembership = membershipRepository.save(freeMembership);

                MvcResult result1 = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.free@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result1.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                mockMvc.perform(post("/register/step4")
                                .session(session)
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/verification-sent"));
        }

        // Handle Step 4 - Redirect to Checkout for Paid Membership
        @Test
        void handleStep4_WithPaidMembership_ShouldRedirectToCheckout() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                MvcResult result1 = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.paid@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result1.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                mockMvc.perform(post("/register/step4")
                                .session(session)
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/checkout"));
        }

        // ======================Negative Tests======================
        // Email Already Exists
        @Test
        void handleStep1_WithExistingEmail_ShouldReturnError() throws Exception {
                User existingUser = new User();
                existingUser.setFirstName("Existing");
                existingUser.setLastName("User");
                existingUser.setEmail("existing@example.com");
                existingUser.setPassword(passwordEncoder.encode("password"));
                existingUser.setPhone("9999999999");
                existingUser.setAddress("123 Existing St");
                existingUser.setCity("Ottawa");
                existingUser.setProvince("ON");
                existingUser.setPostalCode("A1A 1A1");
                existingUser.setRole("USER");
                existingUser.setCreation(new Date());
                userRepository.save(existingUser);

                mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "John")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "existing@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "123 Main St")
                                .param("city", "Ottawa")
                                .param("province", "ON")
                                .param("postalCode", "A1A 1A1"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register"))
                                .andExpect(model().hasErrors());
        }

        // Passwords Don't Match
        @Test
        void handleStep1_WithPasswordMismatch_ShouldReturnError() throws Exception {
                mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "John")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "john@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "DifferentPass123!")
                                .param("phone", "1234567890")
                                .param("address", "123 Main St")
                                .param("city", "Ottawa")
                                .param("province", "ON")
                                .param("postalCode", "A1A 1A1"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register"))
                                .andExpect(model().hasErrors());
        }

        // Access Step 2 Without Session Data
        @Test
        void showStep2_WithoutSessionData_ShouldRedirectToStep1() throws Exception {
                mockMvc.perform(get("/register/step2"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register"));
        }

        // Access Step 3 Without Session Data
        @Test
        void showStep3_ShouldDisplayMembershipOptions() throws Exception {
                mockMvc.perform(get("/register/step3"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register"));
        }

        // Invalid Email Format
        @Test
        void handleStep1_WithInvalidEmail_ShouldReturnError() throws Exception {
                mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "John")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "invalid-email")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "123 Main St")
                                .param("postalCode", "12345"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register"))
                                .andExpect(model().hasErrors());
        }

        // Weak Password
        @Test
        void handleStep1_WithWeakPassword_ShouldReturnError() throws Exception {
                mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "John")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "john@example.com")
                                .param("password", "weak")
                                .param("confirmPassword", "weak")
                                .param("phone", "1234567890")
                                .param("address", "123 Main St")
                                .param("city", "Ottawa")
                                .param("province", "ON")
                                .param("postalCode", "A1A 1A1"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register"))
                                .andExpect(model().hasErrors());
        }

        // Submit Step 3 Without Session Data
        @Test
        void handleStep3_WithoutSessionData_ShouldRedirectToStep1() throws Exception {
                mockMvc.perform(post("/register/step3")
                                .with(csrf())
                                .param("membershipId", "1"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register"));
        }

        // Show Step 4 Without Session Data
        @Test
        void showStep4_WithoutSessionData_ShouldRedirectToStep1() throws Exception {
                mockMvc.perform(get("/register/step4"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register"));
        }

        // Show Step 4 Without Membership Selection
        @Test
        void showStep4_WithoutMembershipSelection_ShouldRedirectToStep3() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(get("/register/step4")
                                .session(session))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step3"));
        }

        // ======================Additional Step 2 Tests======================
        // Handle Step 2 - Add Another Child Action
        @Test
        void handleStep2_WithAddChildAction_ShouldRedirectBackToStep2() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("action", "addChild"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step2"));
        }

        // Handle Step 2 - Skip Children (No Child Data)
        @Test
        void handleStep2_WithoutChildData_ShouldProceedToStep3() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step3"));
        }

        // ======================Additional Step 3 Tests======================
        // Handle Step 3 - Invalid Membership ID
        @Test
        void handleStep3_WithInvalidMembershipId_ShouldProceedToStep4() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                // Non-existent membership ID
                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", "99999"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step4"));
        }

        // ======================Additional Step 4 Tests======================
        // Handle Step 4 - Without Session Data
        @Test
        void handleStep4_WithoutSessionData_ShouldRedirectToRegister() throws Exception {
                mockMvc.perform(post("/register/step4")
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register"));
        }

        // Show Step 4 - With Invalid Membership ID in Session
        @Test
        void showStep4_WithInvalidMembershipId_ShouldRedirectToStep3() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                // Set invalid membership ID
                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", "99999"))
                                .andReturn();

                mockMvc.perform(get("/register/step4")
                                .session(session))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step3"));
        }

        // ======================Checkout Tests======================
        // Show Checkout - With Valid Paid Membership
        @Test
        void showCheckout_WithValidPaidMembership_ShouldDisplayCheckout() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.checkout@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                mockMvc.perform(get("/register/checkout")
                                .session(session))
                                .andExpect(status().isOk())
                                .andExpect(view().name("checkout"))
                                .andExpect(model().attributeExists("membership", "totalAmount", "paypalClientId"));
        }

        // Show Checkout - Without Session Data
        @Test
        void showCheckout_WithoutSessionData_ShouldRedirectToRegister() throws Exception {
                mockMvc.perform(get("/register/checkout"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register"));
        }

        // Show Checkout - With Free Membership (Auto-Complete)
        @Test
        void showCheckout_WithFreeMembership_ShouldAutoCompleteRegistration() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership.setFree(true);
                freeMembership.setDisplayOrder(1);
                Membership savedMembership = membershipRepository.save(freeMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.free.checkout@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                mockMvc.perform(get("/register/checkout")
                                .session(session))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/verification-sent"));
        }

        // Show Checkout - With Invalid Membership ID
        @Test
        void showCheckout_WithInvalidMembershipId_ShouldRedirectToStep3() throws Exception {
                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", "99999"))
                                .andReturn();

                mockMvc.perform(get("/register/checkout")
                                .session(session))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/register/step3"));
        }

        // ======================Email Verification Tests======================
        // Show Verification Sent Page
        @Test
        void showVerificationSent_ShouldDisplayPage() throws Exception {
                mockMvc.perform(get("/register/verification-sent"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("verification-sent"))
                                .andExpect(model().attributeExists("message"));
        }

        // Verify Email - With Valid Token
        @Test
        void verifyEmail_WithValidToken_ShouldVerifySuccessfully() throws Exception {
                User user = User.builder()
                                .firstName("Test")
                                .lastName("User")
                                .email("test.verify@example.com")
                                .password(passwordEncoder.encode("password"))
                                .phone("1234567890")
                                .address("123 Test St")
                                .city("Toronto")
                                .province("ON")
                                .postalCode("M5H 2N2")
                                .role("USER")
                                .emailVerified(false)
                                .creation(new Date())
                                .build();
                user = userRepository.save(user);

                String token = "valid-test-token";
                VerificationToken verificationToken = new VerificationToken(token, user);
                verificationTokenRepository.save(verificationToken);

                mockMvc.perform(get("/register/verify")
                                .param("token", token))
                                .andExpect(status().isOk())
                                .andExpect(view().name("verification-result"))
                                .andExpect(model().attributeExists("success"));

                User verifiedUser = userRepository.findByEmail("test.verify@example.com");
                assert verifiedUser.isEmailVerified();
        }

        // Verify Email - With Invalid Token
        @Test
        void verifyEmail_WithInvalidToken_ShouldShowError() throws Exception {
                mockMvc.perform(get("/register/verify")
                                .param("token", "invalid-token"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("verification-result"))
                                .andExpect(model().attributeExists("error"));
        }

        // Verify Email - With Expired Token
        @Test
        void verifyEmail_WithExpiredToken_ShouldShowError() throws Exception {
                User user = User.builder()
                                .firstName("Test")
                                .lastName("User")
                                .email("test.expired@example.com")
                                .password(passwordEncoder.encode("password"))
                                .phone("1234567890")
                                .address("123 Test St")
                                .city("Toronto")
                                .province("ON")
                                .postalCode("M5H 2N2")
                                .role("USER")
                                .emailVerified(false)
                                .creation(new Date())
                                .build();
                user = userRepository.save(user);

                String token = "expired-test-token";
                VerificationToken verificationToken = new VerificationToken(token, user);

                // Set expiry date to past
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -2); // 2 days ago
                verificationToken.setExpiryDate(cal.getTime());

                verificationTokenRepository.save(verificationToken);

                mockMvc.perform(get("/register/verify")
                                .param("token", token))
                                .andExpect(status().isOk())
                                .andExpect(view().name("verification-result"))
                                .andExpect(model().attributeExists("error"));
        }

        // ======================Resend Verification Tests======================
        // Show Resend Verification Page
        @Test
        void showResendVerification_ShouldDisplayPage() throws Exception {
                mockMvc.perform(get("/register/resend-verification"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("resend-verification"));
        }

        // Resend Verification - With Valid Unverified Email
        @Test
        void resendVerification_WithValidUnverifiedEmail_ShouldResend() throws Exception {
                User user = User.builder()
                                .firstName("Test")
                                .lastName("User")
                                .email("test.resend@example.com")
                                .password(passwordEncoder.encode("password"))
                                .phone("1234567890")
                                .address("123 Test St")
                                .city("Toronto")
                                .province("ON")
                                .postalCode("M5H 2N2")
                                .role("USER")
                                .emailVerified(false)
                                .creation(new Date())
                                .build();
                userRepository.save(user);

                mockMvc.perform(post("/register/resend-verification")
                                .with(csrf())
                                .param("email", "test.resend@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("resend-verification"))
                                .andExpect(model().attributeExists("success"));
        }

        // Resend Verification - With Non-Existent Email
        @Test
        void resendVerification_WithNonExistentEmail_ShouldShowError() throws Exception {
                mockMvc.perform(post("/register/resend-verification")
                                .with(csrf())
                                .param("email", "nonexistent@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("resend-verification"))
                                .andExpect(model().attributeExists("error"));
        }

        // Resend Verification - With Already Verified Email
        @Test
        void resendVerification_WithAlreadyVerifiedEmail_ShouldShowError() throws Exception {
                User user = User.builder()
                                .firstName("Test")
                                .lastName("User")
                                .email("test.verified@example.com")
                                .password(passwordEncoder.encode("password"))
                                .phone("1234567890")
                                .address("123 Test St")
                                .city("Toronto")
                                .province("ON")
                                .postalCode("M5H 2N2")
                                .role("USER")
                                .emailVerified(true)
                                .creation(new Date())
                                .build();
                userRepository.save(user);

                mockMvc.perform(post("/register/resend-verification")
                                .with(csrf())
                                .param("email", "test.verified@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("resend-verification"))
                                .andExpect(model().attributeExists("error"));
        }

        // ======================Additional Validation Tests======================
        // Handle Step 1 - Missing Required Fields
        @Test
        void handleStep1_WithMissingRequiredFields_ShouldReturnError() throws Exception {
                mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "")
                                .param("lastName", "")
                                .param("email", "")
                                .param("password", "")
                                .param("confirmPassword", ""))
                                .andExpect(status().isOk())
                                .andExpect(view().name("register"))
                                .andExpect(model().hasErrors());
        }

        // ======================PayPal Payment Flow Tests======================
        // Create PayPal Order - Success
        @Test
        void createPayPalOrder_WithValidSession_ShouldReturnOrderId() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.paypal@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                // Mock PayPal service
                when(payPalService.createOrderForRegistration(any(Membership.class), any(String.class)))
                                .thenReturn("PAYPAL-ORDER-123");

                String requestBody = "{\"membershipId\":" + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/create-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value("PAYPAL-ORDER-123"));
        }

        // Create PayPal Order - Without Session
        @Test
        void createPayPalOrder_WithoutSession_ShouldReturnUnauthorized() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                String requestBody = "{\"membershipId\":" + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/create-order")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Registration session expired"));
        }

        // Create PayPal Order - Membership Mismatch
        @Test
        void createPayPalOrder_WithMembershipMismatch_ShouldReturnBadRequest() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.mismatch@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                // Send different membership ID than what's in session
                String requestBody = "{\"membershipId\":99999}";

                mockMvc.perform(post("/register/paypal/checkout/create-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Membership mismatch"));
        }

        // Create PayPal Order - Free Membership
        @Test
        void createPayPalOrder_WithFreeMembership_ShouldReturnBadRequest() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership.setActive(true);
                freeMembership.setFree(true);
                freeMembership.setDisplayOrder(1);
                Membership savedMembership = membershipRepository.save(freeMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.free@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                String requestBody = "{\"membershipId\":" + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/create-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Invalid paid membership"));
        }

        // Capture PayPal Order - Success
        @Test
        void capturePayPalOrder_WithValidPayment_ShouldCompleteRegistration() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.capture@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                // Mock create order
                when(payPalService.createOrderForRegistration(any(Membership.class), any(String.class)))
                                .thenReturn("PAYPAL-ORDER-123");

                String createRequestBody = "{\"membershipId\":" + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/create-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody))
                                .andReturn();

                // Mock capture order
                PayPalService.CaptureValidationResult captureResult = new PayPalService.CaptureValidationResult(
                                true, "CAPTURE-123", "COMPLETED", "OK");
                when(payPalService.captureAndValidateRegistration(any(Membership.class), eq("PAYPAL-ORDER-123"),
                                any(String.class)))
                                .thenReturn(captureResult);

                String captureRequestBody = "{\"orderId\":\"PAYPAL-ORDER-123\",\"membershipId\":"
                                + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/capture-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(captureRequestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.redirectUrl").value("/register/verification-sent"));
        }

        // Capture PayPal Order - Without Session
        @Test
        void capturePayPalOrder_WithoutSession_ShouldReturnUnauthorized() throws Exception {
                String captureRequestBody = "{\"orderId\":\"PAYPAL-ORDER-123\",\"membershipId\":1}";

                mockMvc.perform(post("/register/paypal/checkout/capture-order")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(captureRequestBody))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Registration session expired"));
        }

        // Capture PayPal Order - Order ID Mismatch
        @Test
        void capturePayPalOrder_WithOrderIdMismatch_ShouldReturnBadRequest() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.order.mismatch@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                // Mock create order
                when(payPalService.createOrderForRegistration(any(Membership.class), any(String.class)))
                                .thenReturn("PAYPAL-ORDER-123");

                String createRequestBody = "{\"membershipId\":" + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/create-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody))
                                .andReturn();

                // Try to capture with different order ID
                String captureRequestBody = "{\"orderId\":\"DIFFERENT-ORDER-ID\",\"membershipId\":"
                                + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/capture-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(captureRequestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Order mismatch"));
        }

        // Capture PayPal Order - Membership Mismatch
        @Test
        void capturePayPalOrder_WithMembershipMismatch_ShouldReturnBadRequest() throws Exception {
                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership.setActive(true);
                paidMembership.setFree(false);
                paidMembership.setDisplayOrder(2);
                Membership savedMembership = membershipRepository.save(paidMembership);

                MvcResult result = mockMvc.perform(post("/register/step1")
                                .with(csrf())
                                .param("firstName", "Jane")
                                .param("middleName", "")
                                .param("lastName", "Doe")
                                .param("email", "jane.membership.mismatch@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M5H 2N2"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

                mockMvc.perform(post("/register/step2")
                                .session(session)
                                .with(csrf())
                                .param("childName", "Tommy Doe")
                                .param("childAge", "5"))
                                .andReturn();

                mockMvc.perform(post("/register/step3")
                                .session(session)
                                .with(csrf())
                                .param("membershipId", String.valueOf(savedMembership.getId())))
                                .andReturn();

                // Mock create order
                when(payPalService.createOrderForRegistration(any(Membership.class), any(String.class)))
                                .thenReturn("PAYPAL-ORDER-123");

                String createRequestBody = "{\"membershipId\":" + savedMembership.getId() + "}";

                mockMvc.perform(post("/register/paypal/checkout/create-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody))
                                .andReturn();

                // Try to capture with different membership ID
                String captureRequestBody = "{\"orderId\":\"PAYPAL-ORDER-123\",\"membershipId\":99999}";

                mockMvc.perform(post("/register/paypal/checkout/capture-order")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(captureRequestBody))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Membership mismatch"));
        }

}
