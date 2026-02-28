package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.voice.membership.dtos.AdminAddMemberRequest;
import org.voice.membership.dtos.AdminUpdateUserRequest;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminMemberService
 * Tests admin member management operations: create, update, delete
 */
@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private Membership testMembership;
    private User testUser;

    @BeforeEach
    void setUp() {
        testMembership = new Membership();
        testMembership.setId(1);
        testMembership.setName("Premium");

        testUser = new User();
        testUser.setId(10);
        testUser.setEmail("user@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(Role.USER.name());
    }

    // ==================== createMember Tests ====================

    @Test
    void createMember_WithValidData_ShouldCreateUser() {
        // Arrange
        AdminAddMemberRequest request = new AdminAddMemberRequest();
        request.setFirstName("Jane");
        request.setMiddleName("M");
        request.setLastName("Smith");
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        request.setPhone("555-1234");
        request.setAddress("123 Main St");
        request.setPostalCode("A1B2C3");
        request.setEmailVerified(true);
        request.setAccountLocked(false);
        request.setMembershipId(1);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(null);
        when(membershipRepository.findById(1)).thenReturn(Optional.of(testMembership));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User created = adminMemberService.createMember(request);

        // Assert
        assertThat(created).isNotNull();
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("Jane");
        assertThat(savedUser.getMiddleName()).isEqualTo("M");
        assertThat(savedUser.getLastName()).isEqualTo("Smith");
        assertThat(savedUser.getEmail()).isEqualTo("jane@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getPhone()).isEqualTo("555-1234");
        assertThat(savedUser.getAddress()).isEqualTo("123 Main St");
        assertThat(savedUser.getPostalCode()).isEqualTo("A1B2C3");
        assertThat(savedUser.isEmailVerified()).isTrue();
        assertThat(savedUser.isAccountLocked()).isFalse();
        assertThat(savedUser.getMembership()).isEqualTo(testMembership);
        assertThat(savedUser.getRole()).isEqualTo(Role.USER.name());
        assertThat(savedUser.getCreation()).isNotNull();
    }

    @Test
    void createMember_WithExistingEmail_ShouldReturnNull() {
        // Arrange
        AdminAddMemberRequest request = new AdminAddMemberRequest();
        request.setEmail("existing@example.com");

        User existingUser = new User();
        when(userRepository.findByEmail("existing@example.com")).thenReturn(existingUser);

        // Act
        User created = adminMemberService.createMember(request);

        // Assert
        assertThat(created).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    void createMember_WithNullMembershipId_ShouldCreateWithoutMembership() {
        // Arrange
        AdminAddMemberRequest request = new AdminAddMemberRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPassword("pass");
        request.setEmailVerified(false);
        request.setAccountLocked(false);
        request.setMembershipId(null);

        when(userRepository.findByEmail("john@example.com")).thenReturn(null);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User created = adminMemberService.createMember(request);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getMembership()).isNull();
    }

    @Test
    void createMember_WithInvalidMembershipId_ShouldCreateWithoutMembership() {
        // Arrange
        AdminAddMemberRequest request = new AdminAddMemberRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPassword("pass");
        request.setEmailVerified(false);
        request.setAccountLocked(false);
        request.setMembershipId(999);

        when(userRepository.findByEmail("john@example.com")).thenReturn(null);
        when(membershipRepository.findById(999)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User created = adminMemberService.createMember(request);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getMembership()).isNull();
    }

    // ==================== updateMember Tests ====================

    @Test
    void updateMember_WithValidData_ShouldUpdateUser() {
        // Arrange
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFirstName("Updated");
        request.setMiddleName("X");
        request.setLastName("Name");
        request.setEmail("updated@example.com");
        request.setPhone("555-9999");
        request.setAddress("456 New St");
        request.setCity("Toronto");
        request.setProvince("ON");
        request.setPostalCode("M1M1M1");
        request.setMembershipId(1);
        request.setEmailVerified(true);
        request.setAccountLocked(false);

        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(userRepository.findAllByEmailIgnoreCase("updated@example.com")).thenReturn(new ArrayList<>());
        when(membershipRepository.findById(1)).thenReturn(Optional.of(testMembership));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User updated = adminMemberService.updateMember(10, request);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getMiddleName()).isEqualTo("X");
        assertThat(updated.getLastName()).isEqualTo("Name");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getPhone()).isEqualTo("555-9999");
        assertThat(updated.getCity()).isEqualTo("Toronto");
        assertThat(updated.getProvince()).isEqualTo("ON");
        assertThat(updated.getMembership()).isEqualTo(testMembership);
    }

    @Test
    void updateMember_WithNonExistentUser_ShouldReturnNull() {
        // Arrange
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        User updated = adminMemberService.updateMember(999, request);

        // Assert
        assertThat(updated).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMember_WithEmailConflict_ShouldReturnNull() {
        // Arrange
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("conflict@example.com");

        User conflictUser = new User();
        conflictUser.setId(20);

        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(userRepository.findAllByEmailIgnoreCase("conflict@example.com"))
                .thenReturn(List.of(conflictUser));

        // Act
        User updated = adminMemberService.updateMember(10, request);

        // Assert
        assertThat(updated).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMember_WithSameEmail_ShouldAllowUpdate() {
        // Arrange
        testUser.setEmail("same@example.com");
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFirstName("Updated");
        request.setLastName("User");
        request.setEmail("same@example.com");

        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User updated = adminMemberService.updateMember(10, request);

        // Assert
        assertThat(updated).isNotNull();
        verify(userRepository).save(any());
    }

    @Test
    void updateMember_UnlockingAccount_ShouldResetFailedAttempts() {
        // Arrange
        testUser.setAccountLocked(true);
        testUser.setFailedLoginAttempts(5);

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("user@example.com");
        request.setAccountLocked(false);

        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User updated = adminMemberService.updateMember(10, request);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.isAccountLocked()).isFalse();
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updated.getLockoutTime()).isNull();
    }

    // ==================== deleteMember Tests ====================

    @Test
    void deleteMember_WithValidUser_ShouldDeleteAndReturnUser() {
        // Arrange
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        // Act
        User deleted = adminMemberService.deleteMember(10);

        // Assert
        assertThat(deleted).isNotNull();
        assertThat(deleted).isEqualTo(testUser);
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteMember_WithNonExistentUser_ShouldReturnNull() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        User deleted = adminMemberService.deleteMember(999);

        // Assert
        assertThat(deleted).isNull();
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteMember_WithAdminUser_ShouldReturnNull() {
        // Arrange
        testUser.setRole(Role.ADMIN.name());
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        // Act
        User deleted = adminMemberService.deleteMember(10);

        // Assert
        assertThat(deleted).isNull();
        verify(userRepository, never()).delete(any());
    }

    // ==================== formatAdminName Tests ====================

    @Test
    void formatAdminName_WithFullName_ShouldFormatCorrectly() {
        // Arrange
        User admin = new User();
        admin.setFirstName("Jane");
        admin.setMiddleName("Marie");
        admin.setLastName("Doe");

        // Act
        String formatted = adminMemberService.formatAdminName(admin);

        // Assert
        assertThat(formatted).isEqualTo("Jane Marie Doe");
    }

    @Test
    void formatAdminName_WithoutMiddleName_ShouldFormatCorrectly() {
        // Arrange
        User admin = new User();
        admin.setFirstName("John");
        admin.setMiddleName(null);
        admin.setLastName("Smith");

        // Act
        String formatted = adminMemberService.formatAdminName(admin);

        // Assert
        assertThat(formatted).isEqualTo("John Smith");
    }

    @Test
    void formatAdminName_WithEmptyMiddleName_ShouldFormatCorrectly() {
        // Arrange
        User admin = new User();
        admin.setFirstName("Bob");
        admin.setMiddleName("");
        admin.setLastName("Jones");

        // Act
        String formatted = adminMemberService.formatAdminName(admin);

        // Assert
        assertThat(formatted).isEqualTo("Bob Jones");
    }

    @Test
    void formatAdminName_WithNullAdmin_ShouldReturnDefault() {
        // Act
        String formatted = adminMemberService.formatAdminName(null);

        // Assert
        assertThat(formatted).isEqualTo("Admin");
    }
}
