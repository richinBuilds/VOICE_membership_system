package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipPaymentTransaction;
import org.voice.membership.entities.User;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MembershipPaymentTransactionRepository
 * Tests database operations for payment transactions
 */
@DataJpaTest
@ActiveProfiles("test")
class MembershipPaymentTransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MembershipPaymentTransactionRepository paymentTransactionRepository;

    private User testUser1;
    private User testUser2;
    private Membership testMembership1;
    private Membership testMembership2;

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

        testMembership1 = new Membership();
        testMembership1.setName("Basic Membership");
        testMembership1.setPrice(new BigDecimal("29.99"));
        testMembership1.setActive(true);
        testMembership1.setFree(false);
        testMembership1.setDisplayOrder(1);

        testMembership2 = new Membership();
        testMembership2.setName("Premium Membership");
        testMembership2.setPrice(new BigDecimal("99.99"));
        testMembership2.setActive(true);
        testMembership2.setFree(false);
        testMembership2.setDisplayOrder(2);
    }

    @Test
    void findByPaypalOrderId_WithExistingTransaction_ShouldReturnTransaction() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-123456");
        transaction.setPaypalCaptureId("CAPTURE-123456");
        transaction.setAmount(new BigDecimal("29.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("COMPLETED");

        entityManager.persist(transaction);
        entityManager.flush();

        Optional<MembershipPaymentTransaction> found = paymentTransactionRepository.findByPaypalOrderId("ORDER-123456");

        assertThat(found).isPresent();
        assertThat(found.get().getPaypalOrderId()).isEqualTo("ORDER-123456");
        assertThat(found.get().getUser().getEmail()).isEqualTo("john@example.com");
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    void findByPaypalOrderId_WithNonExistentOrderId_ShouldReturnEmpty() {
        Optional<MembershipPaymentTransaction> found = paymentTransactionRepository
                .findByPaypalOrderId("NON-EXISTENT-ORDER");

        assertThat(found).isEmpty();
    }

    @Test
    void findByPaypalCaptureId_WithExistingTransaction_ShouldReturnTransaction() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-789012");
        transaction.setPaypalCaptureId("CAPTURE-789012");
        transaction.setAmount(new BigDecimal("99.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("COMPLETED");

        entityManager.persist(transaction);
        entityManager.flush();

        Optional<MembershipPaymentTransaction> found = paymentTransactionRepository
                .findByPaypalCaptureId("CAPTURE-789012");

        assertThat(found).isPresent();
        assertThat(found.get().getPaypalCaptureId()).isEqualTo("CAPTURE-789012");
        assertThat(found.get().getPaypalOrderId()).isEqualTo("ORDER-789012");
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    void findByPaypalCaptureId_WithNonExistentCaptureId_ShouldReturnEmpty() {
        Optional<MembershipPaymentTransaction> found = paymentTransactionRepository
                .findByPaypalCaptureId("NON-EXISTENT-CAPTURE");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUser_IdAndMembership_IdAndStatus_WithMatchingTransaction_ShouldReturnTrue() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-EXIST-123");
        transaction.setPaypalCaptureId("CAPTURE-EXIST-123");
        transaction.setAmount(new BigDecimal("29.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("COMPLETED");

        entityManager.persist(transaction);
        entityManager.flush();

        boolean exists = paymentTransactionRepository.existsByUser_IdAndMembership_IdAndStatus(
                user.getId(), membership.getId(), "COMPLETED");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUser_IdAndMembership_IdAndStatus_WithDifferentStatus_ShouldReturnFalse() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-STATUS-123");
        transaction.setPaypalCaptureId("CAPTURE-STATUS-123");
        transaction.setAmount(new BigDecimal("29.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("COMPLETED");

        entityManager.persist(transaction);
        entityManager.flush();

        boolean exists = paymentTransactionRepository.existsByUser_IdAndMembership_IdAndStatus(
                user.getId(), membership.getId(), "PENDING");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByUser_IdAndMembership_IdAndStatus_WithNonExistentUser_ShouldReturnFalse() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        boolean exists = paymentTransactionRepository.existsByUser_IdAndMembership_IdAndStatus(
                99999, membership.getId(), "COMPLETED");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByUser_IdAndMembership_IdAndStatus_WithNonExistentMembership_ShouldReturnFalse() {
        User user = entityManager.persist(testUser1);
        entityManager.flush();

        boolean exists = paymentTransactionRepository.existsByUser_IdAndMembership_IdAndStatus(
                user.getId(), 99999, "COMPLETED");

        assertThat(exists).isFalse();
    }

    @Test
    void save_ShouldPersistTransaction() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-SAVE-123");
        transaction.setPaypalCaptureId("CAPTURE-SAVE-123");
        transaction.setAmount(new BigDecimal("49.99"));
        transaction.setCurrency("USD");
        transaction.setStatus("COMPLETED");

        MembershipPaymentTransaction saved = paymentTransactionRepository.save(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPaypalOrderId()).isEqualTo("ORDER-SAVE-123");
        assertThat(saved.getPaypalCaptureId()).isEqualTo("CAPTURE-SAVE-123");
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getCurrency()).isEqualTo("USD");
    }

    @Test
    void save_WithFailedStatus_ShouldPersistWithFailureReason() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-FAIL-123");
        transaction.setPaypalCaptureId(null);
        transaction.setAmount(new BigDecimal("29.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("FAILED");
        transaction.setFailureReason("Insufficient funds");

        MembershipPaymentTransaction saved = paymentTransactionRepository.save(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void findByPaypalOrderId_WithMultipleTransactions_ShouldReturnCorrectOne() {
        User user1 = entityManager.persist(testUser1);
        User user2 = entityManager.persist(testUser2);
        Membership membership1 = entityManager.persist(testMembership1);
        Membership membership2 = entityManager.persist(testMembership2);
        entityManager.flush();

        MembershipPaymentTransaction transaction1 = new MembershipPaymentTransaction();
        transaction1.setUser(user1);
        transaction1.setMembership(membership1);
        transaction1.setPaypalOrderId("ORDER-MULTI-1");
        transaction1.setPaypalCaptureId("CAPTURE-MULTI-1");
        transaction1.setAmount(new BigDecimal("29.99"));
        transaction1.setCurrency("CAD");
        transaction1.setStatus("COMPLETED");

        MembershipPaymentTransaction transaction2 = new MembershipPaymentTransaction();
        transaction2.setUser(user2);
        transaction2.setMembership(membership2);
        transaction2.setPaypalOrderId("ORDER-MULTI-2");
        transaction2.setPaypalCaptureId("CAPTURE-MULTI-2");
        transaction2.setAmount(new BigDecimal("99.99"));
        transaction2.setCurrency("CAD");
        transaction2.setStatus("COMPLETED");

        entityManager.persist(transaction1);
        entityManager.persist(transaction2);
        entityManager.flush();

        Optional<MembershipPaymentTransaction> found1 = paymentTransactionRepository
                .findByPaypalOrderId("ORDER-MULTI-1");
        Optional<MembershipPaymentTransaction> found2 = paymentTransactionRepository
                .findByPaypalOrderId("ORDER-MULTI-2");

        assertThat(found1).isPresent();
        assertThat(found1.get().getUser().getEmail()).isEqualTo("john@example.com");
        assertThat(found1.get().getAmount()).isEqualByComparingTo(new BigDecimal("29.99"));

        assertThat(found2).isPresent();
        assertThat(found2.get().getUser().getEmail()).isEqualTo("jane@example.com");
        assertThat(found2.get().getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    void save_WithDuplicatePaypalOrderId_ShouldThrowException() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        // Create first transaction
        MembershipPaymentTransaction transaction1 = new MembershipPaymentTransaction();
        transaction1.setUser(user);
        transaction1.setMembership(membership);
        transaction1.setPaypalOrderId("DUPLICATE-ORDER-ID");
        transaction1.setPaypalCaptureId("CAPTURE-1");
        transaction1.setAmount(new BigDecimal("49.99"));
        transaction1.setCurrency("CAD");
        transaction1.setStatus("COMPLETED");
        transaction1.setCreatedAt(new Date());
        transaction1.setUpdatedAt(new Date());

        entityManager.persist(transaction1);
        entityManager.flush();
        entityManager.clear();

        // Attempt to create second transaction with same PayPal Order ID
        MembershipPaymentTransaction transaction2 = new MembershipPaymentTransaction();
        transaction2.setUser(user);
        transaction2.setMembership(membership);
        transaction2.setPaypalOrderId("DUPLICATE-ORDER-ID"); // Duplicate
        transaction2.setPaypalCaptureId("CAPTURE-2");
        transaction2.setAmount(new BigDecimal("49.99"));
        transaction2.setCurrency("CAD");
        transaction2.setStatus("COMPLETED");
        transaction2.setCreatedAt(new Date());
        transaction2.setUpdatedAt(new Date());

        // Should throw constraint violation exception
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    entityManager.persist(transaction2);
                    entityManager.flush();
                }
        )).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void save_WithDuplicatePaypalCaptureId_ShouldThrowException() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        // Create first transaction
        MembershipPaymentTransaction transaction1 = new MembershipPaymentTransaction();
        transaction1.setUser(user);
        transaction1.setMembership(membership);
        transaction1.setPaypalOrderId("ORDER-1");
        transaction1.setPaypalCaptureId("DUPLICATE-CAPTURE-ID");
        transaction1.setAmount(new BigDecimal("49.99"));
        transaction1.setCurrency("CAD");
        transaction1.setStatus("COMPLETED");
        transaction1.setCreatedAt(new Date());
        transaction1.setUpdatedAt(new Date());

        entityManager.persist(transaction1);
        entityManager.flush();
        entityManager.clear();

        // Attempt to create second transaction with same Capture ID
        MembershipPaymentTransaction transaction2 = new MembershipPaymentTransaction();
        transaction2.setUser(user);
        transaction2.setMembership(membership);
        transaction2.setPaypalOrderId("ORDER-2");
        transaction2.setPaypalCaptureId("DUPLICATE-CAPTURE-ID"); // Duplicate
        transaction2.setAmount(new BigDecimal("49.99"));
        transaction2.setCurrency("CAD");
        transaction2.setStatus("COMPLETED");
        transaction2.setCreatedAt(new Date());
        transaction2.setUpdatedAt(new Date());

        // Should throw constraint violation exception
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    entityManager.persist(transaction2);
                    entityManager.flush();
                }
        )).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void save_WithNullCaptureId_ShouldSucceed() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        // Create transaction without capture ID (pending status)
        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-NULL-CAPTURE");
        transaction.setPaypalCaptureId(null); // Null is allowed
        transaction.setAmount(new BigDecimal("49.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());

        MembershipPaymentTransaction saved = paymentTransactionRepository.save(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPaypalCaptureId()).isNull();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void save_WithVeryLargeAmount_ShouldHandleCorrectly() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        // Test with large amount (within precision limits)
        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-LARGE-AMOUNT");
        transaction.setPaypalCaptureId("CAPTURE-LARGE");
        transaction.setAmount(new BigDecimal("99999999.99")); // Max for DECIMAL(10,2)
        transaction.setCurrency("CAD");
        transaction.setStatus("COMPLETED");
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());

        MembershipPaymentTransaction saved = paymentTransactionRepository.save(transaction);

        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("99999999.99"));
    }

    @Test
    void save_WithPreciseAmount_ShouldMaintainPrecision() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        // Test precision is maintained (2 decimal places)
        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("ORDER-PRECISE");
        transaction.setPaypalCaptureId("CAPTURE-PRECISE");
        transaction.setAmount(new BigDecimal("49.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("COMPLETED");
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());

        MembershipPaymentTransaction saved = paymentTransactionRepository.save(transaction);
        entityManager.flush();
        entityManager.clear();

        MembershipPaymentTransaction retrieved = paymentTransactionRepository
                .findById(saved.getId()).orElseThrow();

        assertThat(retrieved.getAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(retrieved.getAmount().scale()).isEqualTo(2);
    }

    @Test
    void findByPaypalOrderId_IsCaseSensitive() {
        User user = entityManager.persist(testUser1);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(membership);
        transaction.setPaypalOrderId("Order-CaseSensitive-123");
        transaction.setAmount(new BigDecimal("19.99"));
        transaction.setCurrency("CAD");
        transaction.setStatus("COMPLETED");
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());

        entityManager.persist(transaction);
        entityManager.flush();

        // Should find exact match
        Optional<MembershipPaymentTransaction> found = paymentTransactionRepository
                .findByPaypalOrderId("Order-CaseSensitive-123");
        assertThat(found).isPresent();

        // Should NOT find different case
        Optional<MembershipPaymentTransaction> notFound = paymentTransactionRepository
                .findByPaypalOrderId("order-casesensitive-123");
        assertThat(notFound).isEmpty();
    }
}

