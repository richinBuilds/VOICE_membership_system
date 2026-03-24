package org.voice.membership.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.voice.membership.config.MailchimpProperties;
import org.voice.membership.entities.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Integrates with the Mailchimp Marketing API v3 to keep member contacts
 * in sync with the VOICE Membership System.
 *
 * Responsibilities:
 *  - Add or update a contact in the configured Mailchimp audience (PUT /members/{hash}).
 *  - Update membership and role tags on every sync (POST /members/{hash}/tags).
 *  - Archive (soft-delete) a contact when a member is removed (DELETE /members/{hash}).
 *
 * All calls are fire-and-forget: failures are logged but never propagate to the
 * calling request so that a Mailchimp outage cannot break registration or admin flows.
 *
 * Configuration (application.yaml or env vars):
 *   mailchimp.api-key   = your Mailchimp API key  (env: MAILCHIMP_API_KEY)
 *   mailchimp.list-id   = your Audience / List ID (env: MAILCHIMP_LIST_ID)
 *   mailchimp.enabled   = true | false
 */
@Slf4j
@Service
public class MailchimpService {

    @Autowired
    private MailchimpProperties mailchimpProperties;

    @Autowired
    private ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Add or update a member contact in the Mailchimp audience.
     * Also applies membership-type and role tags.
     *
     * @param user the user to sync
     */
    public void syncContact(User user) {
        if (!isReady()) {
            log.debug("Mailchimp disabled or credentials missing — skipping sync for {}", user.getEmail());
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("email_address", user.getEmail());
            body.put("status_if_new", "subscribed");
            body.put("status", "subscribed");

            ObjectNode mergeFields = objectMapper.createObjectNode();
            mergeFields.put("FNAME", user.getFirstName() != null ? user.getFirstName() : "");
            mergeFields.put("LNAME", user.getLastName() != null ? user.getLastName() : "");
            body.set("merge_fields", mergeFields);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildMemberUrl(user.getEmail())))
                    .header("Authorization", buildAuth())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("Mailchimp: contact synced — {}", user.getEmail());
                updateContactTags(user);
            } else {
                log.warn("Mailchimp: sync failed for {} — HTTP {} — {}",
                        user.getEmail(), response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Mailchimp: unexpected error syncing contact {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Archive (soft-delete) a contact from the Mailchimp audience.
     * Called when an admin deletes a member from the system.
     *
     * @param email the email address of the contact to archive
     */
    public void archiveContact(String email) {
        if (!isReady()) {
            log.debug("Mailchimp disabled or credentials missing — skipping archive for {}", email);
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildMemberUrl(email)))
                    .header("Authorization", buildAuth())
                    .DELETE()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                log.info("Mailchimp: contact archived — {}", email);
            } else {
                log.warn("Mailchimp: archive failed for {} — HTTP {} — {}",
                        email, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Mailchimp: unexpected error archiving contact {}: {}", email, e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Apply membership-type and role tags to an already-synced contact.
     * Uses POST /members/{hash}/tags with status "active" to add each tag.
     */
    private void updateContactTags(User user) {
        try {
            String membershipTag = (user.getMembership() != null)
                    ? user.getMembership().getName() + " Member"
                    : "Free Member";

            var tagsArray = objectMapper.createArrayNode();
            tagsArray.add(objectMapper.createObjectNode()
                    .put("name", membershipTag)
                    .put("status", "active"));

            if (user.getRole() != null) {
                tagsArray.add(objectMapper.createObjectNode()
                        .put("name", user.getRole())
                        .put("status", "active"));
            }

            ObjectNode body = objectMapper.createObjectNode();
            body.set("tags", tagsArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildMemberUrl(user.getEmail()) + "/tags"))
                    .header("Authorization", buildAuth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                log.info("Mailchimp: tags updated for {}", user.getEmail());
            } else {
                log.warn("Mailchimp: tag update failed for {} — HTTP {}",
                        user.getEmail(), response.statusCode());
            }
        } catch (Exception e) {
            log.error("Mailchimp: error updating tags for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /** Returns true when Mailchimp is enabled and real credentials are configured. */
    private boolean isReady() {
        return mailchimpProperties.isEnabled() && mailchimpProperties.hasCredentials();
    }

    /**
     * Builds the full URL for the Mailchimp Members endpoint for a given email.
     * Format: https://{dc}.api.mailchimp.com/3.0/lists/{listId}/members/{md5(email)}
     */
    private String buildMemberUrl(String email) {
        return "https://" + getDataCenter() + ".api.mailchimp.com/3.0/lists/"
                + mailchimpProperties.getListId() + "/members/" + md5(email.toLowerCase());
    }

    /**
     * Extracts the data-centre suffix from the API key.
     * e.g. "abc123-us1" → "us1"
     */
    private String getDataCenter() {
        String key = mailchimpProperties.getApiKey();
        return key.substring(key.lastIndexOf('-') + 1);
    }

    /** Builds the HTTP Basic Authorization header value expected by the Mailchimp API. */
    private String buildAuth() {
        byte[] encoded = Base64.getEncoder()
                .encode(("anystring:" + mailchimpProperties.getApiKey()).getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encoded, StandardCharsets.UTF_8);
    }

    /** Returns the lowercase MD5 hex hash of the input string (used as the subscriber hash). */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}
