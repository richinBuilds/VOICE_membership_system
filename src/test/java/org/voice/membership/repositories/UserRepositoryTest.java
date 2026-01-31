package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserRepository
 * Tests database operations and custom query methods
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .address("123 Main St")
                .postalCode("12345")
                .role(Role.USER.name())
                .creation(new Date())
                .build();

        testUser2 = User.builder()
                .name("Jane Smith")
                .email("Jane@Example.com") // Mixed case
                .password("password456")
                .phone("0987654321")
                .address("456 Oak Ave")
                .postalCode("54321")
                .role(Role.USER.name())
                .creation(new Date())
                .build();
    }

    @Test
    void findByEmail_WithExactMatch_ShouldReturnUser() {
        // Given
        entityManager.persist(testUser1);
        entityManager.flush();

        // When
        User found = userRepository.findByEmail("john@example.com");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("John Doe");
        assertThat(found.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByEmail_WithNonExistentEmail_ShouldReturnNull() {
        // Given
        entityManager.persist(testUser1);
        entityManager.flush();

        // When
        User found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isNull();
    }

    @Test
    void findByEmailIgnoreCase_WithDifferentCase_ShouldReturnUser() {
        // Given
        entityManager.persist(testUser2);
        entityManager.flush();

        // When
        User found = userRepository.findByEmailIgnoreCase("jane@example.com");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Jane Smith");
    }

    @Test
    void findAllByEmailIgnoreCase_WithDuplicateEmails_ShouldReturnAllMatches() {
        // Given
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.flush();

        // When
        List<User> found = userRepository.findAllByEmailIgnoreCase("JANE@EXAMPLE.COM");

        // Then
        assertThat(found).isNotEmpty();
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Jane Smith");
    }

    @Test
    void save_ShouldPersistUser() {
        // When
        User saved = userRepository.save(testUser1);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(entityManager.find(User.class, saved.getId())).isEqualTo(saved);
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Given
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.flush();

        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getName)
                .containsExactlyInAnyOrder("John Doe", "Jane Smith");
    }

    @Test
    void delete_ShouldRemoveUser() {
        // Given
        entityManager.persist(testUser1);
        entityManager.flush();
        int userId = testUser1.getId();

        // When
        userRepository.delete(testUser1);
        entityManager.flush();

        // Then
        User found = entityManager.find(User.class, userId);
        assertThat(found).isNull();
    }

    @Test
    void findById_WithExistingId_ShouldReturnUser() {
        // Given
        entityManager.persist(testUser1);
        entityManager.flush();
        int userId = testUser1.getId();

        // When
        User found = userRepository.findById(userId).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(userId);
    }
}
