package org.voice.membership.controllers;

import org.voice.membership.services.LandingPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/landing-page")
public class LandingPageApiController {

    @Autowired
    private LandingPageService landingPageService;

    /**
     * Get all landing page data as JSON
     */
    @GetMapping("/data")
    public Map<String, Object> getLandingPageData() {
        Map<String, Object> data = new HashMap<>();
        data.put("tagline", landingPageService.getTagline());
        data.put("memberships", landingPageService.getActiveMemberships());
        data.put("benefits", landingPageService.getActiveBenefits());

        // Add login status for modal logic
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal());
        data.put("isUserLoggedIn", String.valueOf(isAuthenticated));

        return data;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Landing page service is running");
        return response;
    }

    /**
     * Initialize default content endpoint
     */
    @GetMapping("/initialize")
    public Map<String, String> initialize() {
        try {
            landingPageService.initializeDefaultContent();
            landingPageService.initializeDefaultMemberships();
            landingPageService.initializeDefaultBenefits();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Default landing page content initialized successfully");
            return response;
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }
}
