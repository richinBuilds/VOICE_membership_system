package org.voice.membership.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "paypal")
public class PayPalProperties {
    private String mode = "sandbox";
    private String clientId;
    private String clientSecret;
    private String webhookId;
    private String currency = "CAD";

    public String getBaseUrl() {
        return "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    public boolean hasCredentials() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
