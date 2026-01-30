package org.voice.membership.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

@Service
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
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    public void sendMembershipUpgradeConfirmation(String to, String userName, String membershipName, String expiryDate) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Membership Upgrade Successful - VOICE");

            String htmlContent = "<html><body>" +
                "<h2>Congratulations " + userName + "!</h2>" +
                "<p>Your membership has been successfully upgraded to <strong>" + membershipName + "</strong>.</p>" +
                "<p><strong>Membership Details:</strong></p>" +
                "<ul>" +
                "<li>Status: Active/Paid</li>" +
                "<li>Expiry Date: " + expiryDate + "</li>" +
                "</ul>" +
                "<p>Thank you for choosing VOICE Membership System!</p>" +
                "<p>Best regards,<br>VOICE Team</p>" +
                "</body></html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            System.out.println("Membership upgrade confirmation email sent to: " + to);
        } catch (MessagingException e) {
            System.err.println("Failed to send membership upgrade email: " + e.getMessage());
            // Don't throw exception - email failure shouldn't break the upgrade process
        }
    }
}
