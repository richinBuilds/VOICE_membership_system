package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.Cart;
import org.voice.membership.entities.User;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CartRepository
 * Tests database operations for shopping carts
 */
@DataJpaTest
@ActiveProfiles("test")
class CartRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

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
                .emailVerified(true)
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
                .emailVerified(true)
                .creation(new Date())
                .build();
    }

    @Test
    void findByUser_WithExistingCart_ShouldReturnCart() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        entityManager.persist(cart);
        entityManager.flush();

        Optional<Cart> found = cartRepository.findByUser(user);

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByUser_WithNoCart_ShouldReturnEmpty() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        Optional<Cart> found = cartRepository.findByUser(user);

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId_WithExistingCart_ShouldReturnCart() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        entityManager.persist(cart);
        entityManager.flush();

        Optional<Cart> found = cartRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void findByUserId_WithNoCart_ShouldReturnEmpty() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        Optional<Cart> found = cartRepository.findByUserId(user.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId_WithInvalidUserId_ShouldReturnEmpty() {
        Optional<Cart> found = cartRepository.findByUserId(99999);

        assertThat(found).isEmpty();
    }

    @Test
    void findByUser_WithMultipleUsers_ShouldReturnCorrectCart() {
        User user1 = entityManager.persist(testUser1);
        User user2 = entityManager.persist(testUser2);
        entityManager.flush();

        Cart cart1 = Cart.builder()
                .user(user1)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        Cart cart2 = Cart.builder()
                .user(user2)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        entityManager.persist(cart1);
        entityManager.persist(cart2);
        entityManager.flush();

        Optional<Cart> found1 = cartRepository.findByUser(user1);
        Optional<Cart> found2 = cartRepository.findByUser(user2);

        assertThat(found1).isPresent();
        assertThat(found1.get().getUser().getEmail()).isEqualTo("john@example.com");
        assertThat(found2).isPresent();
        assertThat(found2.get().getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void save_ShouldPersistCart() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        Date now = new Date();
        Cart cart = Cart.builder()
                .user(user)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Cart saved = cartRepository.save(cart);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void delete_ShouldRemoveCart() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        Integer cartId = cart.getId();
        cartRepository.delete(cart);
        entityManager.flush();

        Optional<Cart> found = cartRepository.findById(cartId);
        assertThat(found).isEmpty();
    }

    @Test
    void update_ShouldUpdateCartTimestamp() throws InterruptedException {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        // Let @PrePersist set the timestamps automatically
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        Integer cartId = cart.getId();
        Date originalCreatedAt = cart.getCreatedAt();
        Date originalUpdatedAt = cart.getUpdatedAt();

        // Sleep to ensure timestamp difference
        Thread.sleep(100);

        // Update the cart - @PreUpdate will set new updatedAt
        Cart foundCart = cartRepository.findById(cartId).orElseThrow();
        foundCart.setUpdatedAt(new Date());
        cartRepository.save(foundCart);
        entityManager.flush();

        Cart updated = cartRepository.findById(cartId).orElseThrow();
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
}
