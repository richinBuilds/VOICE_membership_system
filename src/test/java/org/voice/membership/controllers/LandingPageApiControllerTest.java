package org.voice.membership.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipBenefit;
import org.voice.membership.services.LandingPageService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LandingPageApiController
 * Tests REST API endpoints for landing page data retrieval and initialization
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LandingPageApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LandingPageService landingPageService;

    private List<Membership> mockMemberships;
    private List<MembershipBenefit> mockBenefits;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public LandingPageService landingPageService() {
            return mock(LandingPageService.class);
        }
    }

    @BeforeEach
    void setUp() {
        reset(landingPageService);

        // Create mock memberships
        Membership freeMembership = new Membership();
        freeMembership.setId(1);
        freeMembership.setName("Free");
        freeMembership.setFree(true);
        freeMembership.setActive(true);
        freeMembership.setPrice(null);
        freeMembership.setDescription("Free membership");
        freeMembership.setFeatures("Basic access");
        freeMembership.setDisplayOrder(1);

        Membership premiumMembership = new Membership();
        premiumMembership.setId(2);
        premiumMembership.setName("Premium");
        premiumMembership.setFree(false);
        premiumMembership.setActive(true);
        premiumMembership.setPrice(new BigDecimal("20.00"));
        premiumMembership.setDescription("Premium membership");
        premiumMembership.setFeatures("Full access");
        premiumMembership.setDisplayOrder(2);

        mockMemberships = Arrays.asList(freeMembership, premiumMembership);

        // Create mock benefits
        MembershipBenefit benefit1 = new MembershipBenefit();
        benefit1.setTitle("Benefit 1");
        benefit1.setDescription("Description 1");
        benefit1.setIcon("icon-1");
        benefit1.setActive(true);
        benefit1.setDisplayOrder(1);

        MembershipBenefit benefit2 = new MembershipBenefit();
        benefit2.setTitle("Benefit 2");
        benefit2.setDescription("Description 2");
        benefit2.setIcon("icon-2");
        benefit2.setActive(true);
        benefit2.setDisplayOrder(2);

        mockBenefits = Arrays.asList(benefit1, benefit2);
    }

    // ==================== GET /api/landing-page/data Tests ====================

    @Test
    void getLandingPageData_WithAuthenticatedUser_ShouldReturnDataWithLoggedInTrue() throws Exception {
        // Arrange
        when(landingPageService.getTagline()).thenReturn("Welcome to VOICE");
        when(landingPageService.getActiveMemberships()).thenReturn(mockMemberships);
        when(landingPageService.getActiveBenefits()).thenReturn(mockBenefits);

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/data")
                        .with(user("test@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Welcome to VOICE"))
                .andExpect(jsonPath("$.isUserLoggedIn").value("true"))
                .andExpect(jsonPath("$.memberships").isArray())
                .andExpect(jsonPath("$.memberships.length()").value(2))
                .andExpect(jsonPath("$.memberships[0].name").value("Free"))
                .andExpect(jsonPath("$.memberships[1].name").value("Premium"))
                .andExpect(jsonPath("$.benefits").isArray())
                .andExpect(jsonPath("$.benefits.length()").value(2));

        verify(landingPageService, times(1)).getTagline();
        verify(landingPageService, times(1)).getActiveMemberships();
        verify(landingPageService, times(1)).getActiveBenefits();
    }

    @Test
    void getLandingPageData_WithUnauthenticatedUser_ShouldReturnDataWithLoggedInFalse() throws Exception {
        // Arrange
        when(landingPageService.getTagline()).thenReturn("Welcome to VOICE");
        when(landingPageService.getActiveMemberships()).thenReturn(mockMemberships);
        when(landingPageService.getActiveBenefits()).thenReturn(mockBenefits);

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Welcome to VOICE"))
                .andExpect(jsonPath("$.isUserLoggedIn").value("false"))
                .andExpect(jsonPath("$.memberships").isArray())
                .andExpect(jsonPath("$.memberships.length()").value(2))
                .andExpect(jsonPath("$.benefits").isArray())
                .andExpect(jsonPath("$.benefits.length()").value(2));

        verify(landingPageService, times(1)).getTagline();
        verify(landingPageService, times(1)).getActiveMemberships();
        verify(landingPageService, times(1)).getActiveBenefits();
    }

    @Test
    void getLandingPageData_WithAdminUser_ShouldReturnDataWithLoggedInTrue() throws Exception {
        // Arrange
        when(landingPageService.getTagline()).thenReturn("Welcome to VOICE");
        when(landingPageService.getActiveMemberships()).thenReturn(mockMemberships);
        when(landingPageService.getActiveBenefits()).thenReturn(mockBenefits);

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/data")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Welcome to VOICE"))
                .andExpect(jsonPath("$.isUserLoggedIn").value("true"))
                .andExpect(jsonPath("$.memberships").isArray())
                .andExpect(jsonPath("$.benefits").isArray());

        verify(landingPageService, times(1)).getTagline();
        verify(landingPageService, times(1)).getActiveMemberships();
        verify(landingPageService, times(1)).getActiveBenefits();
    }

    @Test
    void getLandingPageData_WithEmptyMemberships_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        when(landingPageService.getTagline()).thenReturn("Welcome to VOICE");
        when(landingPageService.getActiveMemberships()).thenReturn(Arrays.asList());
        when(landingPageService.getActiveBenefits()).thenReturn(mockBenefits);

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Welcome to VOICE"))
                .andExpect(jsonPath("$.isUserLoggedIn").value("false"))
                .andExpect(jsonPath("$.memberships").isArray())
                .andExpect(jsonPath("$.memberships.length()").value(0))
                .andExpect(jsonPath("$.benefits").isArray())
                .andExpect(jsonPath("$.benefits.length()").value(2));
    }

    @Test
    void getLandingPageData_WithEmptyBenefits_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        when(landingPageService.getTagline()).thenReturn("Welcome to VOICE");
        when(landingPageService.getActiveMemberships()).thenReturn(mockMemberships);
        when(landingPageService.getActiveBenefits()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").value("Welcome to VOICE"))
                .andExpect(jsonPath("$.isUserLoggedIn").value("false"))
                .andExpect(jsonPath("$.memberships").isArray())
                .andExpect(jsonPath("$.memberships.length()").value(2))
                .andExpect(jsonPath("$.benefits").isArray())
                .andExpect(jsonPath("$.benefits.length()").value(0));
    }

    @Test
    void getLandingPageData_WithNullTagline_ShouldReturnNullTagline() throws Exception {
        // Arrange
        when(landingPageService.getTagline()).thenReturn(null);
        when(landingPageService.getActiveMemberships()).thenReturn(mockMemberships);
        when(landingPageService.getActiveBenefits()).thenReturn(mockBenefits);

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagline").isEmpty())
                .andExpect(jsonPath("$.isUserLoggedIn").value("false"))
                .andExpect(jsonPath("$.memberships").isArray())
                .andExpect(jsonPath("$.benefits").isArray());
    }

    // ==================== GET /api/landing-page/health Tests ====================

    @Test
    void health_ShouldReturnOkStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/landing-page/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.message").value("Landing page service is running"));

        // Health endpoint doesn't call service methods
        verifyNoInteractions(landingPageService);
    }

    @Test
    void health_WithAuthenticatedUser_ShouldReturnOkStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/landing-page/health")
                        .with(user("test@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.message").value("Landing page service is running"));

        verifyNoInteractions(landingPageService);
    }

    // ==================== GET /api/landing-page/initialize Tests ====================

    @Test
    void initialize_WithSuccessfulInitialization_ShouldReturnSuccess() throws Exception {
        // Arrange
        doNothing().when(landingPageService).initializeDefaultContent();
        doNothing().when(landingPageService).initializeDefaultMemberships();
        doNothing().when(landingPageService).initializeDefaultBenefits();

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Default landing page content initialized successfully"));

        verify(landingPageService, times(1)).initializeDefaultContent();
        verify(landingPageService, times(1)).initializeDefaultMemberships();
        verify(landingPageService, times(1)).initializeDefaultBenefits();
    }

    @Test
    void initialize_WithContentInitializationError_ShouldReturnError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database connection failed"))
                .when(landingPageService).initializeDefaultContent();

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Database connection failed"));

        verify(landingPageService, times(1)).initializeDefaultContent();
        // Should not call subsequent methods if first one fails
        verify(landingPageService, never()).initializeDefaultMemberships();
        verify(landingPageService, never()).initializeDefaultBenefits();
    }

    @Test
    void initialize_WithMembershipInitializationError_ShouldReturnError() throws Exception {
        // Arrange
        doNothing().when(landingPageService).initializeDefaultContent();
        doThrow(new RuntimeException("Membership initialization failed"))
                .when(landingPageService).initializeDefaultMemberships();

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Membership initialization failed"));

        verify(landingPageService, times(1)).initializeDefaultContent();
        verify(landingPageService, times(1)).initializeDefaultMemberships();
        // Should not call benefits initialization if memberships fail
        verify(landingPageService, never()).initializeDefaultBenefits();
    }

    @Test
    void initialize_WithBenefitInitializationError_ShouldReturnError() throws Exception {
        // Arrange
        doNothing().when(landingPageService).initializeDefaultContent();
        doNothing().when(landingPageService).initializeDefaultMemberships();
        doThrow(new RuntimeException("Benefit initialization failed"))
                .when(landingPageService).initializeDefaultBenefits();

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Benefit initialization failed"));

        verify(landingPageService, times(1)).initializeDefaultContent();
        verify(landingPageService, times(1)).initializeDefaultMemberships();
        verify(landingPageService, times(1)).initializeDefaultBenefits();
    }

    @Test
    void initialize_WithAuthenticatedUser_ShouldReturnSuccess() throws Exception {
        // Arrange
        doNothing().when(landingPageService).initializeDefaultContent();
        doNothing().when(landingPageService).initializeDefaultMemberships();
        doNothing().when(landingPageService).initializeDefaultBenefits();

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/initialize")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Default landing page content initialized successfully"));

        verify(landingPageService, times(1)).initializeDefaultContent();
        verify(landingPageService, times(1)).initializeDefaultMemberships();
        verify(landingPageService, times(1)).initializeDefaultBenefits();
    }

    @Test
    void initialize_WithNullPointerException_ShouldReturnError() throws Exception {
        // Arrange
        doThrow(new NullPointerException("Required configuration is missing"))
                .when(landingPageService).initializeDefaultContent();

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Required configuration is missing"));

        verify(landingPageService, times(1)).initializeDefaultContent();
    }

    @Test
    void initialize_WithIllegalStateException_ShouldReturnError() throws Exception {
        // Arrange
        doNothing().when(landingPageService).initializeDefaultContent();
        doThrow(new IllegalStateException("Content already initialized"))
                .when(landingPageService).initializeDefaultMemberships();

        // Act & Assert
        mockMvc.perform(get("/api/landing-page/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Content already initialized"));

        verify(landingPageService, times(1)).initializeDefaultContent();
        verify(landingPageService, times(1)).initializeDefaultMemberships();
    }
}
