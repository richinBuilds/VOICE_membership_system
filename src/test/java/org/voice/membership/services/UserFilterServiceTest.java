package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserFilterService
 * Tests complex user filtering logic based on various criteria
 */
class UserFilterServiceTest {

    private UserFilterService userFilterService;
    private List<User> testUsers;

    @BeforeEach
    void setUp() throws Exception {
        userFilterService = new UserFilterService();

        // Create test memberships
        Membership freeMembership = new Membership();
        freeMembership.setId(1);
        freeMembership.setName("Free");
        freeMembership.setFree(true);

        Membership paidMembership = new Membership();
        paidMembership.setId(2);
        paidMembership.setName("Premium");
        paidMembership.setFree(false);

        // Create test users
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        User user1 = new User();
        user1.setId(1);
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setAddress("123 Main St");
        user1.setCity("Toronto");
        user1.setProvince("ON");
        user1.setPostalCode("M1M1M1");
        user1.setCreation(sdf.parse("2024-01-15"));
        user1.setMembership(paidMembership);

        Child child1a = Child.builder()
                .id(1)
                .name("Child 1A")
                .age(5)
                .hearingLossType("Sensorineural")
                .equipmentType("Hearing Aid")
                .user(user1)
                .build();
        user1.setChildren(new ArrayList<>(List.of(child1a)));

        User user2 = new User();
        user2.setId(2);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setAddress("456 Oak Ave");
        user2.setCity("Vancouver");
        user2.setProvince("BC");
        user2.setPostalCode("V5V5V5");
        user2.setCreation(sdf.parse("2024-03-20"));
        user2.setMembership(freeMembership);

        Child child2a = Child.builder()
                .id(2)
                .name("Child 2A")
                .age(8)
                .hearingLossType("Conductive")
                .equipmentType("Cochlear Implant")
                .user(user2)
                .build();
        user2.setChildren(new ArrayList<>(List.of(child2a)));

        User user3 = new User();
        user3.setId(3);
        user3.setFirstName("Bob");
        user3.setLastName("Johnson");
        user3.setAddress("789 Elm Rd");
        user3.setCity("Montreal");
        user3.setProvince("QC");
        user3.setPostalCode("H1H1H1");
        user3.setCreation(sdf.parse("2024-06-10"));
        user3.setMembership(null);

        Child child3a = Child.builder()
                .id(3)
                .name("Child 3A")
                .age(12)
                .hearingLossType("Mixed")
                .equipmentType("FM System")
                .user(user3)
                .build();
        user3.setChildren(new ArrayList<>(List.of(child3a)));

        testUsers = new ArrayList<>(Arrays.asList(user1, user2, user3));
    }

    // ==================== Address Filter Tests ====================

    @Test
    void filterUsers_ByAddress_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, "Main St", null, null, null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getAddress()).contains("Main St");
    }

    @Test
    void filterUsers_ByPostalCode_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, "V5V5V5", null, null, null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getPostalCode()).isEqualTo("V5V5V5");
    }

    @Test
    void filterUsers_ByAddressPartial_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, "Elm", null, null, null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getAddress()).contains("Elm");
    }

    @Test
    void filterUsers_WithNullAddress_ShouldReturnAllUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(testUsers.size());
    }

    // ==================== City Filter Tests ====================

    @Test
    void filterUsers_ByCity_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, "Toronto", null, null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getCity()).isEqualTo("Toronto");
    }

    @Test
    void filterUsers_ByCityPartial_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, "van", null, null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getCity()).containsIgnoringCase("van");
    }

    // ==================== Province Filter Tests ====================

    @Test
    void filterUsers_ByProvince_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, "ON", null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getProvince()).isEqualTo("ON");
    }

    @Test
    void filterUsers_ByProvincePartial_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, "bc", null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getProvince()).isEqualToIgnoringCase("bc");
    }

    // ==================== Child Age Filter Tests ====================

    @Test
    void filterUsers_ByMinAge_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, 10, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getChildren().get(0).getAge()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void filterUsers_ByMaxAge_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, 6, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getChildren().get(0).getAge()).isLessThanOrEqualTo(6);
    }

    @Test
    void filterUsers_ByAgeRange_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, 6, 10, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getChildren().get(0).getAge()).isBetween(6, 10);
    }

    @Test
    void filterUsers_WithoutChildren_ShouldBeExcludedByAgeFilter() {
        // Arrange
        User userWithoutChildren = new User();
        userWithoutChildren.setId(99);
        userWithoutChildren.setChildren(new ArrayList<>());
        testUsers.add(userWithoutChildren);

        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, 5, 10, null, null, null, null, null);

        // Assert
        assertThat(filtered).doesNotContain(userWithoutChildren);
    }

    // ==================== Hearing Loss Type Filter Tests ====================

    @Test
    void filterUsers_ByHearingLossType_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, "Sensorineural", null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getChildren().get(0).getHearingLossType()).isEqualTo("Sensorineural");
    }

    @Test
    void filterUsers_ByHearingLossTypeCaseInsensitive_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, "conductive", null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getChildren().get(0).getHearingLossType())
                .isEqualToIgnoringCase("conductive");
    }

    // ==================== Equipment Type Filter Tests ====================

    @Test
    void filterUsers_ByEquipmentType_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, "Hearing Aid", null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getChildren().get(0).getEquipmentType()).isEqualTo("Hearing Aid");
    }

    @Test
    void filterUsers_ByEquipmentTypeCaseInsensitive_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, "cochlear implant", null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getChildren().get(0).getEquipmentType())
                .isEqualToIgnoringCase("cochlear implant");
    }

    // ==================== Registration Date Filter Tests ====================

    @Test
    void filterUsers_ByStartDate_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, "2024-03-01", null, null);

        // Assert
        assertThat(filtered).hasSize(2); // user2 and user3
    }

    @Test
    void filterUsers_ByEndDate_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, null, "2024-03-31", null);

        // Assert
        assertThat(filtered).hasSize(2); // user1 and user2
    }

    @Test
    void filterUsers_ByDateRange_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, "2024-02-01", "2024-05-31", null);

        // Assert
        assertThat(filtered).hasSize(1); // only user2
    }

    @Test
    void filterUsers_WithInvalidDate_ShouldReturnAllUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, "invalid-date", null, null);

        // Assert
        assertThat(filtered).hasSize(testUsers.size());
    }

    // ==================== Payment Status Filter Tests ====================

    @Test
    void filterUsers_ByPaidStatus_ShouldReturnPaidMembers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, null, null, "paid");

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getMembership()).isNotNull();
        assertThat(filtered.get(0).getMembership().isFree()).isFalse();
    }

    @Test
    void filterUsers_ByUnpaidStatus_ShouldReturnUnpaidMembers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, null, null, "unpaid");

        // Assert
        assertThat(filtered).hasSize(2); // user2 (free) and user3 (null)
    }

    // ==================== Combined Filters Tests ====================

    @Test
    void filterUsers_WithMultipleFilters_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, "Toronto", "ON", null, null, null, null, null, null, "paid");

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getCity()).isEqualTo("Toronto");
        assertThat(filtered.get(0).getProvince()).isEqualTo("ON");
        assertThat(filtered.get(0).getMembership().isFree()).isFalse();
    }

    @Test
    void filterUsers_WithAllNullFilters_ShouldReturnAllUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, null, null, null, null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(testUsers.size());
    }

    @Test
    void filterUsers_WithCombinedChildFilters_ShouldReturnMatchingUsers() {
        // Act
        List<User> filtered = userFilterService.filterUsers(
                testUsers, null, null, null, 8, 12, "Mixed", null, null, null, null);

        // Assert
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getId()).isEqualTo(3);
    }
}
