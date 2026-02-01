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
                .firstName("John")
                .middleName(null)
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .address("123 Main St")
                .postalCode("12345")
                .role(Role.USER.name())
                .creation(new Date())
                .build();

        testUser2 = User.builder()
                .firstName("Jane")
                .middleName(null)
                .lastName("Smith")
                .email("Jane@Example.com")
                .password("password456")
                .phone("0987654321")
                .address("456 Oak Ave")
                .postalCode("54321")
                .role(Role.USER.name())
                .creation(new Date())
                .build();
    }

    // Test 1: Find user by exact email address
    @Test
    void findByEmail_WithExactMatch_ShouldReturnUser() {
        entityManager.persist(testUser1);
        entityManager.flush();

        User found = userRepository.findByEmail("john@example.com");

        assertThat(found).isNotNull();
        assertThat(found.getFirstName()).isEqualTo("John");
        assertThat(found.getLastName()).isEqualTo("Doe");
        assertThat(found.getEmail()).isEqualTo("john@example.com");
    }

    // Test 2: Search for non-existent email returns null
    @Test
    void findByEmail_WithNonExistentEmail_ShouldReturnNull() {
        entityManager.persist(testUser1);
        entityManager.flush();

        User found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isNull();
    }

    // Test 3: Find user by email (case-insensitive)
    @Test
    void findByEmailIgnoreCase_WithDifferentCase_ShouldReturnUser() {
        entityManager.persist(testUser2);
        entityManager.flush();

        User found = userRepository.findByEmailIgnoreCase("jane@example.com");

        assertThat(found).isNotNull();
        assertThat(found.getFirstName()).isEqualTo("Jane");
        assertThat(found.getLastName()).isEqualTo("Smith");
    }

    // Test 4: Find all users with matching email (case-insensitive)
    @Test
    void findAllByEmailIgnoreCase_WithDuplicateEmails_ShouldReturnAllMatches() {
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.flush();

        List<User> found = userRepository.findAllByEmailIgnoreCase("JANE@EXAMPLE.COM");

        assertThat(found).isNotEmpty();
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFirstName()).isEqualTo("Jane");
        assertThat(found.get(0).getLastName()).isEqualTo("Smith");
    }

    // Test 5: Save user to database
    @Test
    void save_ShouldPersistUser() {
        User saved = userRepository.save(testUser1);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(entityManager.find(User.class, saved.getId())).isEqualTo(saved);
    }

    // Test 6: Retrieve all users from database
    @Test
    void findAll_ShouldReturnAllUsers() {
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.flush();
        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
        assertThat(users).extracting(User::getLastName)
                .containsExactlyInAnyOrder("Doe", "Smith");
    }

    // Test 7: Delete user from database
    @Test
    void delete_ShouldRemoveUser() {
        entityManager.persist(testUser1);
        entityManager.flush();
        int userId = testUser1.getId();
        userRepository.delete(testUser1);
        entityManager.flush();

        User found = entityManager.find(User.class, userId);
        assertThat(found).isNull();
    }

    // Test 8: Find user by ID from database
    @Test
    void findById_WithExistingId_ShouldReturnUser() {
        entityManager.persist(testUser1);
        entityManager.flush();
        int userId = testUser1.getId();
        User found = userRepository.findById(userId).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(userId);
    }
}
