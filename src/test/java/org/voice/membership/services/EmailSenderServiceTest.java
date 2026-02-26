package org.voice.membership.services;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailSenderService
 * Tests email sending functionality for password reset, verification, and
 * custom emails
 */
@ExtendWith(MockitoExtension.class)
class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailSenderService emailSenderService;

    @Captor
    private ArgumentCaptor<Context> contextCaptor;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ==================== Password Reset Email Tests ====================

    @Test
    void sendPasswordResetEmail_WithValidData_ShouldSendEmail() {
        // Arrange
        String to = "user@example.com";
        String resetLink = "https://example.com/reset?token=abc123";
        String htmlContent = "<html><body>Reset your password</body></html>";

        when(templateEngine.process(eq("reset-password-email"), any(Context.class)))
                .thenReturn(htmlContent);

        // Act
        emailSenderService.sendPasswordResetEmail(to, resetLink);

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("reset-password-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("resetLink")).isEqualTo(resetLink);
    }

    @Test
    void sendPasswordResetEmail_WhenMessagingException_ShouldThrowRuntimeException() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");
        // Simulate mail server failure
        doThrow(new MailSendException("Mail server error"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailSenderService.sendPasswordResetEmail("user@example.com", "link"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send password reset email");
    }

    // ==================== Verification Email Tests ====================

    @Test
    void sendVerificationEmail_WithValidData_ShouldSendEmail() {
        // Arrange
        String to = "newuser@example.com";
        String userName = "John Doe";
        String verificationLink = "https://example.com/verify?token=xyz789";
        String htmlContent = "<html><body>Verify your email</body></html>";

        when(templateEngine.process(eq("email-verification"), any(Context.class)))
                .thenReturn(htmlContent);

        // Act
        emailSenderService.sendVerificationEmail(to, userName, verificationLink);

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("email-verification"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("userName")).isEqualTo(userName);
        assertThat(capturedContext.getVariable("verificationLink")).isEqualTo(verificationLink);
    }

    @Test
    void sendVerificationEmail_WhenMessagingException_ShouldThrowRuntimeException() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");
        // Simulate mail server failure
        doThrow(new MailSendException("Mail server error"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailSenderService.sendVerificationEmail("user@example.com", "User", "link"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send verification email");
    }
}