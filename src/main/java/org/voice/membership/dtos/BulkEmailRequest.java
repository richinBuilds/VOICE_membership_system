package org.voice.membership.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO for sending bulk emails to selected members.
 * Contains recipient IDs, email subject, and message body.
 */
public class BulkEmailRequest {

    @NotEmpty(message = "At least one recipient must be selected")
    private List<Integer> recipientIds;

    @NotBlank(message = "Subject cannot be empty")
    private String subject;

    @NotBlank(message = "Message body cannot be empty")
    private String messageBody;

    public BulkEmailRequest() {
    }

    public BulkEmailRequest(List<Integer> recipientIds, String subject, String messageBody) {
        this.recipientIds = recipientIds;
        this.subject = subject;
        this.messageBody = messageBody;
    }

    public List<Integer> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<Integer> recipientIds) {
        this.recipientIds = recipientIds;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
