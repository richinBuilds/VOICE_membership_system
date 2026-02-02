package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Tests user authentication, password reset functionality
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1)
                .firstName("Test")
                .middleName(null)
                .lastName("User")
                .email("test@example.com")
                .password("encodedPassword123")
                .phone("1234567890")
                .address("123 Test St")
                .postalCode("12345")
                .role(Role.USER.name())
                .creation(new Date())
                .build();
    }

    @Test
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        UserDetails userDetails = userService.loadUserByUsername("test@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsername_WithInvalidEmail_ShouldThrowException() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        assertThatThrownBy(() -> userService.loadUserByUsername("invalid@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: invalid@example.com");
    }

    @Test
    void sendPasswordResetEmail_WithValidEmail_ShouldReturnTrue() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        doNothing().when(emailSenderService).sendPasswordResetEmail(anyString(), anyString());

        boolean result = userService.sendPasswordResetEmail("test@example.com");

        assertThat(result).isTrue();
        verify(userRepository).findByEmail("test@example.com");
        verify(emailSenderService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void sendPasswordResetEmail_WithInvalidEmail_ShouldReturnFalse() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        boolean result = userService.sendPasswordResetEmail("invalid@example.com");

        assertThat(result).isFalse();
        verify(userRepository).findByEmail("invalid@example.com");
        verify(emailSenderService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_WithValidToken_ShouldReturnTrue() {
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(testUser);
        doNothing().when(emailSenderService).sendPasswordResetEmail(anyString(), anyString());

        boolean result = userService.sendPasswordResetEmail(email);

        assertThat(result).isTrue();
        verify(userRepository).findByEmail(email);
        verify(emailSenderService).sendPasswordResetEmail(eq(email), anyString());
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldReturnFalse() {
        String invalidToken = "invalid-token";
        String newPassword = "NewPassword123!";

        boolean result = userService.resetPassword(invalidToken, newPassword);

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loadUserByUsername_WithAdminRole_ShouldReturnAdminUser() {
        User adminUser = User.builder()
                .id(2)
                .firstName("Admin")
                .middleName(null)
                .lastName("User")
                .email("admin@example.com")
                .password("adminPassword")
                .role(Role.ADMIN.name())
                .creation(new Date())
                .build();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser);

        UserDetails userDetails = userService.loadUserByUsername("admin@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin@example.com");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().toString()).contains("ADMIN");
    }
}

