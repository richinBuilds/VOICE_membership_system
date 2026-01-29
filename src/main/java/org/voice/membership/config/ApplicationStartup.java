package org.voice.membership.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.voice.membership.services.LandingPageService;

/**
 * ApplicationStartup Component
 * Runs initialization tasks when the application starts up.
 * This ensures that default membership options and landing page content are created
 * if they don't already exist in the database.
 */
@Slf4j
@Component
public class ApplicationStartup implements CommandLineRunner {

    @Autowired
    private LandingPageService landingPageService;

    /**
     * Runs when Spring Boot application starts
     * Initializes default memberships and landing page content
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application data...");
        
        try {
            // Initialize default membership options (Free and Premium)
            landingPageService.initializeDefaultMemberships();
            log.info("Memberships initialized successfully");
            
            // Initialize default landing page content
            landingPageService.initializeDefaultContent();
            log.info("Landing page content initialized successfully");
            
            // Initialize default benefits
            landingPageService.initializeDefaultBenefits();
            log.info("Benefits initialized successfully");
            
            log.info("Application startup initialization complete!");
        } catch (Exception e) {
            log.error("Error during application startup initialization: {}", e.getMessage(), e);
        }
    }
}
