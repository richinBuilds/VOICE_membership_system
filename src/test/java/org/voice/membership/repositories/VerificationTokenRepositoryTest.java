package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.User;
import org.voice.membership.entities.VerificationToken;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for VerificationTokenRepository
 * Tests database operations for email verification tokens
 */
@DataJpaTest
@ActiveProfiles("test")
class VerificationTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .address("123 Main St")
                .city("Toronto")
                .province("ON")
                .postalCode("M5H 2N2")
                .role("USER")
                .emailVerified(false)
                .creation(new Date())
                .build();

        testUser2 = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .password("password456")
                .phone("0987654321")
                .address("456 Oak Ave")
                .city("Vancouver")
                .province("BC")
                .postalCode("V6B 1A1")
                .role("USER")
                .emailVerified(false)
                .creation(new Date())
                .build();
    }

    @Test
    void findByToken_WithExistingToken_ShouldReturnToken() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        VerificationToken token = new VerificationToken("test-token-123", user);
        entityManager.persist(token);
        entityManager.flush();

        Optional<VerificationToken> found = verificationTokenRepository.findByToken("test-token-123");

        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("test-token-123");
        assertThat(found.get().getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByToken_WithNonExistentToken_ShouldReturnEmpty() {
        Optional<VerificationToken> found = verificationTokenRepository.findByToken("non-existent-token");

        assertThat(found).isEmpty();
    }

    @Test
    void findByUser_WithExistingToken_ShouldReturnToken() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        VerificationToken token = new VerificationToken("user-token-456", user);
        entityManager.persist(token);
        entityManager.flush();

        Optional<VerificationToken> found = verificationTokenRepository.findByUser(user);

        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("user-token-456");
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void findByUser_WithNoToken_ShouldReturnEmpty() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        Optional<VerificationToken> found = verificationTokenRepository.findByUser(user);

        assertThat(found).isEmpty();
    }

    @Test
    void findByUser_WithMultipleUsers_ShouldReturnCorrectToken() {
        User user1 = entityManager.persist(testUser1);
        User user2 = entityManager.persist(testUser2);
        entityManager.flush();

        VerificationToken token1 = new VerificationToken("token-user1", user1);
        VerificationToken token2 = new VerificationToken("token-user2", user2);
        entityManager.persist(token1);
        entityManager.persist(token2);
        entityManager.flush();

        Optional<VerificationToken> found1 = verificationTokenRepository.findByUser(user1);
        Optional<VerificationToken> found2 = verificationTokenRepository.findByUser(user2);

        assertThat(found1).isPresent();
        assertThat(found1.get().getToken()).isEqualTo("token-user1");
        assertThat(found2).isPresent();
        assertThat(found2.get().getToken()).isEqualTo("token-user2");
    }

    @Test
    void deleteByUser_WithExistingToken_ShouldDeleteToken() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        VerificationToken token = new VerificationToken("delete-token", user);
        entityManager.persist(token);
        entityManager.flush();

        verificationTokenRepository.deleteByUser(user);
        entityManager.flush();

        Optional<VerificationToken> found = verificationTokenRepository.findByUser(user);
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByUser_WithNoToken_ShouldNotThrowException() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        // Should not throw exception even if no token exists
        verificationTokenRepository.deleteByUser(user);
        entityManager.flush();

        Optional<VerificationToken> found = verificationTokenRepository.findByUser(user);
        assertThat(found).isEmpty();
    }

    @Test
    void save_ShouldPersistTokenWithExpiryDate() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        VerificationToken token = new VerificationToken("expiry-token", user);
        VerificationToken saved = verificationTokenRepository.save(token);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getExpiryDate()).isNotNull();
        assertThat(saved.getExpiryDate()).isAfter(new Date());
    }

    @Test
    void isExpired_WithExpiredToken_ShouldReturnTrue() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        VerificationToken token = new VerificationToken("expired-token", user);
        // Set expiry date to past
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -25); // 25 hours ago
        token.setExpiryDate(cal.getTime());

        entityManager.persist(token);
        entityManager.flush();

        Optional<VerificationToken> found = verificationTokenRepository.findByToken("expired-token");

        assertThat(found).isPresent();
        assertThat(found.get().isExpired()).isTrue();
    }

    @Test
    void isExpired_WithValidToken_ShouldReturnFalse() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        VerificationToken token = new VerificationToken("valid-token", user);
        entityManager.persist(token);
        entityManager.flush();

        Optional<VerificationToken> found = verificationTokenRepository.findByToken("valid-token");

        assertThat(found).isPresent();
        assertThat(found.get().isExpired()).isFalse();
    }
}
