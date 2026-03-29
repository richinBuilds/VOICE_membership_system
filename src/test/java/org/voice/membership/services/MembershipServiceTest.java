package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MembershipService
 * Tests membership expiry calculations, date formatting, and expiry status
 * checks
 */
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminNotificationService adminNotificationService;

    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipService(membershipRepository, userRepository, adminNotificationService);
    }

    // ==================== calculateMembershipExpiry Tests ====================

    @Test
    void calculateMembershipExpiry_WithValidDate_ShouldAddOneYear() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 15, 10, 30, 0);
        Date startDate = cal.getTime();

        // Act
        Date expiryDate = membershipService.calculateMembershipExpiry(startDate);

        // Assert
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTime(startDate);
        expectedCal.add(Calendar.YEAR, 1);

        assertThat(expiryDate).isNotNull();
        assertThat(expiryDate.getTime()).isEqualTo(expectedCal.getTime().getTime());
    }

    @Test
    void calculateMembershipExpiry_WithNullDate_ShouldReturnNull() {
        // Act
        Date expiryDate = membershipService.calculateMembershipExpiry(null);

        // Assert
        assertThat(expiryDate).isNull();
    }

    @Test
    void calculateMembershipExpiry_WithLeapYearDate_ShouldHandleCorrectly() {
        // Arrange - Feb 29, 2024 (leap year)
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 29, 0, 0, 0);
        Date startDate = cal.getTime();

        // Act
        Date expiryDate = membershipService.calculateMembershipExpiry(startDate);

        // Assert - Should be Feb 28 or 29, 2025 depending on Java's handling
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTime(startDate);
        expectedCal.add(Calendar.YEAR, 1);

        assertThat(expiryDate).isNotNull();
        assertThat(expiryDate.getTime()).isEqualTo(expectedCal.getTime().getTime());
    }

    @Test
    void calculateMembershipExpiry_WithCurrentDate_ShouldCalculateCorrectly() {
        // Arrange
        Date now = new Date();

        // Act
        Date expiryDate = membershipService.calculateMembershipExpiry(now);

        // Assert
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTime(now);
        expectedCal.add(Calendar.YEAR, 1);

        assertThat(expiryDate).isNotNull();
        assertThat(expiryDate.getTime()).isEqualTo(expectedCal.getTime().getTime());
    }

    // ==================== formatMembershipDate Tests ====================

    @Test
    void formatMembershipDate_WithValidDate_ShouldReturnFormattedString() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 15, 10, 30, 0);
        Date date = cal.getTime();

        // Act
        String formatted = membershipService.formatMembershipDate(date);

        // Assert
        assertThat(formatted).isEqualTo("January 15, 2025");
    }

    @Test
    void formatMembershipDate_WithNullDate_ShouldReturnDash() {
        // Act
        String formatted = membershipService.formatMembershipDate(null);

        // Assert
        assertThat(formatted).isEqualTo("-");
    }

    @Test
    void formatMembershipDate_WithDecemberDate_ShouldFormatCorrectly() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.DECEMBER, 31, 23, 59, 59);
        Date date = cal.getTime();

        // Act
        String formatted = membershipService.formatMembershipDate(date);

        // Assert
        assertThat(formatted).isEqualTo("December 31, 2024");
    }

    @Test
    void formatMembershipDate_WithSingleDigitDay_ShouldFormatCorrectly() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.MARCH, 5, 0, 0, 0);
        Date date = cal.getTime();

        // Act
        String formatted = membershipService.formatMembershipDate(date);

        // Assert
        assertThat(formatted).isEqualTo("March 05, 2025");
    }

    // ==================== isMembershipExpired Tests ====================

    @Test
    void isMembershipExpired_WithPastDate_ShouldReturnTrue() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1); // Yesterday
        Date pastDate = cal.getTime();

        // Act
        boolean expired = membershipService.isMembershipExpired(pastDate);

        // Assert
        assertThat(expired).isTrue();
    }

    @Test
    void isMembershipExpired_WithFutureDate_ShouldReturnFalse() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1); // Tomorrow
        Date futureDate = cal.getTime();

        // Act
        boolean expired = membershipService.isMembershipExpired(futureDate);

        // Assert
        assertThat(expired).isFalse();
    }

    @Test
    void isMembershipExpired_WithNullDate_ShouldReturnFalse() {
        // Act
        boolean expired = membershipService.isMembershipExpired(null);

        // Assert
        assertThat(expired).isFalse();
    }

    @Test
    void isMembershipExpired_WithVeryOldDate_ShouldReturnTrue() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2020, Calendar.JANUARY, 1, 0, 0, 0);
        Date oldDate = cal.getTime();

        // Act
        boolean expired = membershipService.isMembershipExpired(oldDate);

        // Assert
        assertThat(expired).isTrue();
    }

    @Test
    void isMembershipExpired_WithFarFutureDate_ShouldReturnFalse() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2030, Calendar.DECEMBER, 31, 23, 59, 59);
        Date futureDate = cal.getTime();

        // Act
        boolean expired = membershipService.isMembershipExpired(futureDate);

        // Assert
        assertThat(expired).isFalse();
    }

    // ==================== Integration Tests ====================

    @Test
    void calculateAndFormat_ShouldWorkTogether() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JUNE, 15, 10, 0, 0);
        Date startDate = cal.getTime();

        // Act
        Date expiryDate = membershipService.calculateMembershipExpiry(startDate);
        String formatted = membershipService.formatMembershipDate(expiryDate);

        // Assert
        assertThat(formatted).isEqualTo("June 15, 2025");
    }

    @Test
    void calculateAndCheckExpiry_WithFutureStart_ShouldNotBeExpired() {
        // Arrange
        Date now = new Date();

        // Act
        Date expiryDate = membershipService.calculateMembershipExpiry(now);
        boolean expired = membershipService.isMembershipExpired(expiryDate);

        // Assert
        assertThat(expired).isFalse(); // One year from now should not be expired
    }
}
