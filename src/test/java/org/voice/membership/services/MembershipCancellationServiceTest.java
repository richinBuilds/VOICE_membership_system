package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.voice.membership.entities.User;
import org.voice.membership.entities.Membership;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.MembershipRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MembershipCancellationService
 */
@DisplayName("Membership Cancellation Service Tests")
class MembershipCancellationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private MembershipCancellationService cancellationService;

    private User testUser;
    private Membership paidMembership;
    private Membership freeMembership;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize free membership
        freeMembership = Membership.builder()
                .id(1)
                .name("Free Membership")
                .description("Basic features")
                .price(BigDecimal.ZERO)
                .isFree(true)
                .active(true)
                .build();

        // Initialize paid membership
        paidMembership = Membership.builder()
                .id(2)
                .name("Premium Membership")
                .description("All premium features")
                .price(new BigDecimal("20.00"))
                .isFree(false)
                .active(true)
                .build();

        // Initialize test user with paid membership
        testUser = User.builder()
                .id(1)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .membership(paidMembership)
                .membershipStartDate(new Date())
                .build();
    }

    @Test
    @DisplayName("Should successfully cancel paid membership and downgrade to free")
    void testCancelPaidMembershipSuccessfully() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(membershipRepository.findByIsFree(true)).thenReturn(Arrays.asList(freeMembership));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        var result = cancellationService.cancelMembership(1);

        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("Premium Membership"));
        assertEquals(freeMembership, testUser.getMembership());
        assertNotNull(testUser.getMembershipStartDate());
        assertNull(testUser.getMembershipExpiryDate());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should return error when trying to cancel free membership")
    void testCannotCancelFreeMembership() {
        testUser.setMembership(freeMembership);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        var result = cancellationService.cancelMembership(1);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Free memberships cannot be cancelled"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return error when user not found")
    void testCancelMembershipUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        var result = cancellationService.cancelMembership(999);

        assertFalse(result.isSuccess());
        assertEquals("User not found", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return error when user has no membership")
    void testCancelMembershipNoActiveMembership() {
        testUser.setMembership(null);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        var result = cancellationService.cancelMembership(1);

        assertFalse(result.isSuccess());
        assertEquals("No active membership to cancel", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set membership to null when no free membership exists")
    void testCancelMembershipNoFreeMembershipAvailable() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(membershipRepository.findByIsFree(true)).thenReturn(Arrays.asList());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        var result = cancellationService.cancelMembership(1);

        assertTrue(result.isSuccess());
        assertNull(testUser.getMembership());
        assertNull(testUser.getMembershipStartDate());
        assertNull(testUser.getMembershipExpiryDate());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should return true when user can cancel paid membership")
    void testCanCancelMembershipReturnsTrue() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        boolean canCancel = cancellationService.canCancelMembership(1);

        assertTrue(canCancel);
    }

    @Test
    @DisplayName("Should return false when user has free membership")
    void testCannotCancelFreeMembershipCheck() {
        testUser.setMembership(freeMembership);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        boolean canCancel = cancellationService.canCancelMembership(1);

        assertFalse(canCancel);
    }

    @Test
    @DisplayName("Should return false when user cannot cancel membership")
    void testCanCancelMembershipReturnsFalse() {
        testUser.setMembership(null);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        boolean canCancel = cancellationService.canCancelMembership(1);

        assertFalse(canCancel);
    }

    @Test
    @DisplayName("Should return false when user not found for can cancel check")
    void testCanCancelMembershipUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        boolean canCancel = cancellationService.canCancelMembership(999);

        assertFalse(canCancel);
    }

    @Test
    @DisplayName("Should get current membership info successfully")
    void testGetCurrentMembershipInfo() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        var info = cancellationService.getCurrentMembershipInfo(1);

        assertNotNull(info);
        assertEquals("Premium Membership", info.getName());
        assertFalse(info.isFree());
        assertEquals("All premium features", info.getDescription());
    }

    @Test
    @DisplayName("Should return no membership info when user has no membership")
    void testGetCurrentMembershipInfoNoMembership() {
        testUser.setMembership(null);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        var info = cancellationService.getCurrentMembershipInfo(1);

        assertNotNull(info);
        assertNull(info.getName());
        assertFalse(info.isFree());
        assertEquals("No membership", info.getDescription());
    }

    @Test
    @DisplayName("Should return no membership info when user not found")
    void testGetCurrentMembershipInfoUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        var info = cancellationService.getCurrentMembershipInfo(999);

        assertNotNull(info);
        assertNull(info.getName());
        assertEquals("No membership", info.getDescription());
    }

    @Test
    @DisplayName("Should get free membership info correctly")
    void testGetFreeMembershipInfo() {
        testUser.setMembership(freeMembership);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        var info = cancellationService.getCurrentMembershipInfo(1);

        assertNotNull(info);
        assertEquals("Free Membership", info.getName());
        assertTrue(info.isFree());
        assertEquals("Basic features", info.getDescription());
    }

    @Test
    @DisplayName("CancellationResult should have correct properties")
    void testCancellationResultProperties() {
        var result = new MembershipCancellationService.CancellationResult(true, "Success message");

        assertTrue(result.isSuccess());
        assertEquals("Success message", result.getMessage());
    }

    @Test
    @DisplayName("MembershipInfo should have correct properties")
    void testMembershipInfoProperties() {
        var info = new MembershipCancellationService.MembershipInfo("Test Membership", false, "Test description");

        assertEquals("Test Membership", info.getName());
        assertFalse(info.isFree());
        assertEquals("Test description", info.getDescription());
    }
}
