package org.voice.membership.config;

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
        System.out.println("Initializing application data...");
        
        try {
            // Initialize default membership options (Free and Premium)
            landingPageService.initializeDefaultMemberships();
            System.out.println("Memberships initialized successfully");
            
            // Initialize default landing page content
            landingPageService.initializeDefaultContent();
            System.out.println("Landing page content initialized successfully");
            
            // Initialize default benefits
            landingPageService.initializeDefaultBenefits();
            System.out.println("Benefits initialized successfully");
            
            System.out.println("Application startup initialization complete!");
        } catch (Exception e) {
            System.err.println("Error during application startup initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
