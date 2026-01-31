package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for page accessibility
 * Tests that all major pages load without errors
 * 
 * NOTE: These are basic smoke tests to ensure pages are accessible.
 * For detailed functionality testing, see controller tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PageAccessibilitySmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        User existingUser = userRepository.findByEmail("integration-test@example.com");
        if (existingUser != null) {
            userRepository.delete(existingUser);
        }
    }

    // Test: Verify landing/home page loads successfully and contains VOICE branding
    @Test
    void testLandingPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("VOICE");
    }

    // Test: Verify registration page loads and displays account creation form
    @Test
    void testRegistrationPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/register", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Create Your Account");
    }

    // Test: Verify login page loads successfully
    @Test
    void testLoginPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/login", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Login");
    }

    // Test: Verify unauthenticated users are redirected to login when accessing
    // profile
    @Test
    void testProfilePageRequiresAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/profile", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Login");
    }

    // Test: Verify admin dashboard requires authentication and redirects to login
    @Test
    void testAdminDashboardRequiresAdminRole() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/admin/dashboard", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Login");
    }

    // Test: Verify forgot password page is accessible and displays correctly
    @Test
    void testForgotPasswordPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/forgot-password", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Forgot Password");
    }

    // Test: Verify Spring application context loads successfully and beans are
    // available
    @Test
    void testApplicationContextLoads() {
        assertThat(restTemplate).isNotNull();
        assertThat(userRepository).isNotNull();
    }

    // Test: Verify database connection is working and queries can be executed
    @Test
    void testDatabaseConnection() {
        long count = userRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
