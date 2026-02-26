package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.Cart;
import org.voice.membership.entities.CartItem;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CartItemRepository
 * Tests database operations for cart items
 */
@DataJpaTest
@ActiveProfiles("test")
class CartItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartItemRepository cartItemRepository;

    private User testUser;
    private Cart testCart1;
    private Cart testCart2;
    private Membership testMembership1;
    private Membership testMembership2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
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
    void findByCart_WithItems_ShouldReturnAllItems() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        CartItem item1 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("29.99"))
                .build();
        CartItem item2 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("59.98"))
                .build();

        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.flush();

        List<CartItem> found = cartItemRepository.findByCart(cart);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(CartItem::getQuantity).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void findByCart_WithNoItems_ShouldReturnEmptyList() {
        User user = entityManager.persist(testUser);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        List<CartItem> found = cartItemRepository.findByCart(cart);

        assertThat(found).isEmpty();
    }

    @Test
    void findByCartId_WithItems_ShouldReturnAllItems() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        CartItem item = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(3)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("89.97"))
                .build();
        entityManager.persist(item);
        entityManager.flush();

        List<CartItem> found = cartItemRepository.findByCartId(cart.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getQuantity()).isEqualTo(3);
        assertThat(found.get(0).getTotalPrice()).isEqualByComparingTo(new BigDecimal("89.97"));
    }

    @Test
    void findByCartId_WithInvalidCartId_ShouldReturnEmptyList() {
        List<CartItem> found = cartItemRepository.findByCartId(99999);

        assertThat(found).isEmpty();
    }

    @Test
    void deleteByCart_WithItems_ShouldDeleteAllItems() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        CartItem item1 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("29.99"))
                .build();
        CartItem item2 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("59.98"))
                .build();

        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.flush();

        cartItemRepository.deleteByCart(cart);
        entityManager.flush();

        List<CartItem> found = cartItemRepository.findByCart(cart);
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByCartId_WithItems_ShouldDeleteAllItems() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        CartItem item = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("29.99"))
                .build();
        entityManager.persist(item);
        entityManager.flush();

        Integer cartId = cart.getId();
        cartItemRepository.deleteByCartId(cartId);
        entityManager.flush();

        List<CartItem> found = cartItemRepository.findByCartId(cartId);
        assertThat(found).isEmpty();
    }

    @Test
    void findByCart_WithMultipleCarts_ShouldReturnCorrectItems() {
        User user1 = entityManager.persist(testUser);

        // Create second user since each user can only have one cart (unique constraint)
        User user2 = User.builder()
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
        user2 = entityManager.persist(user2);

        Membership membership1 = entityManager.persist(testMembership1);
        Membership membership2 = entityManager.persist(testMembership2);
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
        cart1 = entityManager.persist(cart1);
        cart2 = entityManager.persist(cart2);
        entityManager.flush();

        CartItem item1 = CartItem.builder()
                .cart(cart1)
                .membership(membership1)
                .quantity(1)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("29.99"))
                .build();
        CartItem item2 = CartItem.builder()
                .cart(cart2)
                .membership(membership2)
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .totalPrice(new BigDecimal("199.98"))
                .build();

        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.flush();

        List<CartItem> foundCart1 = cartItemRepository.findByCart(cart1);
        List<CartItem> foundCart2 = cartItemRepository.findByCart(cart2);

        assertThat(foundCart1).hasSize(1);
        assertThat(foundCart1.get(0).getMembership().getName()).isEqualTo("Basic Membership");
        assertThat(foundCart2).hasSize(1);
        assertThat(foundCart2.get(0).getMembership().getName()).isEqualTo("Premium Membership");
    }

    @Test
    void save_ShouldPersistCartItem() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        CartItem item = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(5)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("149.95"))
                .build();

        CartItem saved = cartItemRepository.save(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getQuantity()).isEqualTo(5);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo(new BigDecimal("149.95"));
    }

    @Test
    void update_ShouldModifyCartItem() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        CartItem item = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("29.99"))
                .build();
        item = entityManager.persist(item);
        entityManager.flush();

        Integer itemId = item.getId();

        // Update quantity and price
        CartItem foundItem = cartItemRepository.findById(itemId).orElseThrow();
        foundItem.setQuantity(3);
        foundItem.setTotalPrice(new BigDecimal("89.97"));
        cartItemRepository.save(foundItem);
        entityManager.flush();

        CartItem updated = cartItemRepository.findById(itemId).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(3);
        assertThat(updated.getTotalPrice()).isEqualByComparingTo(new BigDecimal("89.97"));
    }

    @Test
    void deleteByCart_ShouldRemoveAllItemsFromCart() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        CartItem item1 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("29.99"))
                .build();
        CartItem item2 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("59.98"))
                .build();

        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.flush();

        List<CartItem> itemsBefore = cartItemRepository.findByCart(cart);
        assertThat(itemsBefore).hasSize(2);

        cartItemRepository.deleteByCart(cart);
        entityManager.flush();

        List<CartItem> itemsAfter = cartItemRepository.findByCart(cart);
        assertThat(itemsAfter).isEmpty();
    }

    @Test
    void deleteByCartId_ShouldRemoveAllItemsFromCart() {
        User user = entityManager.persist(testUser);
        Membership membership = entityManager.persist(testMembership1);
        entityManager.flush();

        Cart cart = Cart.builder()
                .user(user)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        cart = entityManager.persist(cart);
        entityManager.flush();

        Integer cartId = cart.getId();

        CartItem item1 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("29.99"))
                .build();
        CartItem item2 = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(3)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("89.97"))
                .build();

        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.flush();

        List<CartItem> itemsBefore = cartItemRepository.findByCartId(cartId);
        assertThat(itemsBefore).hasSize(2);

        cartItemRepository.deleteByCartId(cartId);
        entityManager.flush();

        List<CartItem> itemsAfter = cartItemRepository.findByCartId(cartId);
        assertThat(itemsAfter).isEmpty();
    }
}
