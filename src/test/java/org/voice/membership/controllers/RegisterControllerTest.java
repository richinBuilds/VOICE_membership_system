package org.voice.membership.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.*;

import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                                .param("address", "456 Oak Ave")
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
                                .andExpect(redirectedUrl("/profile"));
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
                                .param("name", "Jane Doe")
                                .param("email", "jane@example.com")
                                .param("password", "ValidPass123!")
                                .param("confirmPassword", "ValidPass123!")
                                .param("phone", "1234567890")
                                .param("address", "456 Oak Ave")
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

}
