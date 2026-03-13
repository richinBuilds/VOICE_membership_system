package org.voice.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the VOICE Membership Spring Boot application.
 * Boots the application and initializes all configured components.
 */
@SpringBootApplication
@EnableScheduling
public class WebRegistrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebRegistrationApplication.class, args);
    }

}

