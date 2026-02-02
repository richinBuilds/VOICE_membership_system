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

    @Test
    void testLandingPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("VOICE");
    }

    @Test
    void testRegistrationPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/register", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Create Your Account");
    }

    @Test
    void testLoginPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/login", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Login");
    }

    // profile
    @Test
    void testProfilePageRequiresAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/profile", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Login");
    }

    @Test
    void testAdminDashboardRequiresAdminRole() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/admin/dashboard", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Login");
    }

    @Test
    void testForgotPasswordPageAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/forgot-password", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Forgot Password");
    }

    // available
    @Test
    void testApplicationContextLoads() {
        assertThat(restTemplate).isNotNull();
        assertThat(userRepository).isNotNull();
    }

    @Test
    void testDatabaseConnection() {
        long count = userRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}

