package org.voice.membership.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Service
/**
 * Sends application emails such as password reset and verification notices.
 * Uses Thymeleaf templates and JavaMail to build and deliver messages.
 */
public class EmailSenderService {

    private static final int CUSTOM_EMAIL_MAX_ATTEMPTS = 3;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    @Lazy
    private LandingPageService landingPageService;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    @Value("${app.email.custom.min-interval-ms:1200}")
    private long customEmailMinIntervalMs;

    private final Object customEmailRateLock = new Object();
    private long lastCustomEmailSentAtMs = 0L;

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
        String recipient = to == null ? "" : to.trim();
        String senderName = fromName == null ? "Admin" : fromName.trim();

        for (int attempt = 1; attempt <= CUSTOM_EMAIL_MAX_ATTEMPTS; attempt++) {
            throttleCustomEmailSends();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(recipient);
                helper.setSubject(subject);

                if (mailFromAddress != null && !mailFromAddress.isBlank()) {
                    helper.setFrom(mailFromAddress.trim());
                }

                String htmlContent = "<html><body>" +
                        "<p>" + messageBody.replaceAll("\n", "<br>") + "</p>" +
                        "<br>" +
                        "<p>Sent from: " + senderName + "</p>" +
                        "<p>---<br>VOICE Membership System</p>" +
                        "</body></html>";

                helper.setText(htmlContent, true);
                mailSender.send(mimeMessage);
                return;
            } catch (MessagingException | MailException e) {
                if (attempt < CUSTOM_EMAIL_MAX_ATTEMPTS) {
                    long backoffMillis = 500L * attempt;
                    if (isRateLimitError(e)) {
                        backoffMillis = Math.max(customEmailMinIntervalMs * (attempt + 1), 1500L);
                    }
                    log.warn("Custom email send attempt {}/{} failed for {}. Retrying in {} ms. Cause: {}",
                            attempt, CUSTOM_EMAIL_MAX_ATTEMPTS, recipient, backoffMillis, e.getMessage());
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Failed to send custom email to " + recipient, e);
                    }
                    continue;
                }
                throw new RuntimeException("Failed to send custom email to " + recipient, e);
            }
        }
    }

    private void throttleCustomEmailSends() {
        long interval = Math.max(customEmailMinIntervalMs, 250L);
        synchronized (customEmailRateLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastCustomEmailSentAtMs;
            long waitMillis = interval - elapsed;
            if (waitMillis > 0) {
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Custom email send interrupted while waiting for rate limit", e);
                }
            }
            lastCustomEmailSentAtMs = System.currentTimeMillis();
        }
    }

    private boolean isRateLimitError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("too many emails per second")
                        || lower.contains("mailtrap.io/billing/plans/testing")
                        || lower.contains("550 5.7.0")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Send a membership renewal reminder email to a paid member approaching expiry.
     *
     * @param to              Recipient email address
     * @param userName        Member's first name
     * @param membershipName  Name of the membership plan (e.g. "Premium
     *                        Membership")
     * @param expiryDate      Human-readable expiry date string (e.g. "April 10,
     *                        2026")
     * @param daysUntilExpiry Number of days remaining until expiry
     * @param renewalUrl      URL to the renewal / upgrade page
     */
    public void sendRenewalReminderEmail(String to, String userName, String membershipName,
            String expiryDate, long daysUntilExpiry, String renewalUrl) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);

            // Use admin-editable subject from DB; fall back to default if blank
            String rawSubject = landingPageService.getRenewalEmailSubject();
            String subject = (rawSubject != null && !rawSubject.isBlank())
                    ? rawSubject
                    : "Membership Renewal Reminder - VOICE";
            subject = replacePlaceholders(subject, userName, membershipName, expiryDate, daysUntilExpiry, renewalUrl);
            helper.setSubject(subject);

            // Build template context — always use the styled HTML template
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("membershipName", membershipName);
            context.setVariable("expiryDate", expiryDate);
            context.setVariable("daysUntilExpiry", daysUntilExpiry);
            context.setVariable("renewalUrl", renewalUrl);

            // If admin has set a custom body, convert it to HTML paragraphs and inject into
            // template
            String rawBody = landingPageService.getRenewalEmailBody();
            if (rawBody != null && !rawBody.isBlank()) {
                String bodyText = replacePlaceholders(rawBody, userName, membershipName, expiryDate, daysUntilExpiry,
                        renewalUrl);
                String customBodyHtml = Arrays.stream(bodyText.split("\n{2,}"))
                        .map(para -> "<p style='color:#4B5563;font-size:15px;line-height:1.7;margin:0 0 14px 0;'>" +
                                para.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n",
                                        "<br>")
                                +
                                "</p>")
                        .collect(Collectors.joining());
                context.setVariable("customBodyHtml", customBodyHtml);
            }

            String htmlContent = templateEngine.process("membership-renewal-reminder", context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException("Failed to send renewal reminder email to " + to, e);
        }
    }

    private String replacePlaceholders(String template, String memberName, String membershipName,
            String expiryDate, long daysUntilExpiry, String renewalUrl) {
        return template
                .replace("{memberName}", memberName)
                .replace("{membershipName}", membershipName)
                .replace("{expiryDate}", expiryDate)
                .replace("{daysUntilExpiry}", String.valueOf(daysUntilExpiry))
                .replace("{renewalUrl}", renewalUrl);
    }
}
