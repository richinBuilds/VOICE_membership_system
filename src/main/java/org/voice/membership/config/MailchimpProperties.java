package org.voice.membership.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Mailchimp Marketing API integration.
 * Set mailchimp.api-key and mailchimp.list-id in application.yaml or
 * via environment variables MAILCHIMP_API_KEY / MAILCHIMP_LIST_ID.
 */
@Component
@ConfigurationProperties(prefix = "mailchimp")
public class MailchimpProperties {

    /** Mailchimp Marketing API key (ends with the data-center suffix, e.g. -us1). */
    private String apiKey;

    /** Mailchimp Audience / List ID to sync contacts into. */
    private String listId;

    /** Master switch — set to false to disable all Mailchimp calls without removing config. */
    private boolean enabled = true;

    /**
     * Returns true only when the API key and list ID look like real credentials
     * (not the placeholder strings written in application.yaml).
     */
    public boolean hasCredentials() {
        return apiKey != null && !apiKey.isBlank()
                && !apiKey.contains("your-mailchimp")
                && listId != null && !listId.isBlank()
                && !listId.contains("your-audience");
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getListId() { return listId; }
    public void setListId(String listId) { this.listId = listId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
