package org.voice.membership.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.voice.membership.services.LandingPageService;

/**
 * Runs initialization tasks when the application starts up.
 * This ensures that default membership options and landing page content are
 * created
 * if they don't already exist in the database.
 */
@Slf4j
@Component
public class ApplicationStartup implements CommandLineRunner {

    @Autowired
    private LandingPageService landingPageService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application data...");

        try {
            landingPageService.initializeDefaultMemberships();
            log.info("Memberships initialized successfully");

            landingPageService.initializeDefaultContent();
            log.info("Landing page content initialized successfully");

            landingPageService.initializeDefaultBenefits();
            log.info("Benefits initialized successfully");

            log.info("Application startup initialization complete!");
        } catch (Exception e) {
            log.error("Error during application startup initialization: {}", e.getMessage(), e);
        }
    }
}
