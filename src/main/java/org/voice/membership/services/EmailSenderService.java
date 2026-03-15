package org.voice.membership.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

@Service
/**
 * Sends application emails such as password reset and verification notices.
 * Uses Thymeleaf templates and JavaMail to build and deliver messages.
 */
public class EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendPasswordResetEmail(String to, String resetLink) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Password Reset Request");

            Context context = new Context();
            context.setVariable("resetLink", resetLink);
            String htmlContent = templateEngine.process("reset-password-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendVerificationEmail(String to, String userName, String verificationLink) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Verify Your Email - VOICE Membership");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("verificationLink", verificationLink);
            String htmlContent = templateEngine.process("email-verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendCustomEmail(String to, String subject, String messageBody, String fromName) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = "<html><body>" +
                    "<p>" + messageBody.replaceAll("\n", "<br>") + "</p>" +
                    "<br>" +
                    "<p>Sent from: " + fromName + "</p>" +
                    "<p>---<br>VOICE Membership System</p>" +
                    "</body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException("Failed to send custom email to " + to, e);
        }
    }

    /**
     * Send a membership renewal reminder email to a paid member approaching expiry.
     *
     * @param to              Recipient email address
     * @param userName        Member's first name
     * @param membershipName  Name of the membership plan (e.g. "Premium Membership")
     * @param expiryDate      Human-readable expiry date string (e.g. "April 10, 2026")
     * @param daysUntilExpiry Number of days remaining until expiry
     * @param renewalUrl      URL to the renewal / upgrade page
     */
    public void sendRenewalReminderEmail(String to, String userName, String membershipName,
                                         String expiryDate, long daysUntilExpiry, String renewalUrl) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Membership Renewal Reminder - VOICE");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("membershipName", membershipName);
            context.setVariable("expiryDate", expiryDate);
            context.setVariable("daysUntilExpiry", daysUntilExpiry);
            context.setVariable("renewalUrl", renewalUrl);

            String htmlContent = templateEngine.process("membership-renewal-reminder", context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException("Failed to send renewal reminder email to " + to, e);
        }
    }
}
