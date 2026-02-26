package org.voice.membership.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides a mock JavaMailSender
 * to prevent actual email sending during tests
 */
@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        
        // Mock createMimeMessage response
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> {
            Session session = Session.getInstance(System.getProperties());
            return new MimeMessage(session);
        });
        
        // send() does nothing (no actual emails sent)
        
        return mailSender;
    }
}
