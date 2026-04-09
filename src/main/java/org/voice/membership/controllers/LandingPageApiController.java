package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.voice.membership.dtos.ApiResponse;
import org.voice.membership.dtos.LandingPageDataResponse;
import org.voice.membership.services.LandingPageApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/landing-page")
@RequiredArgsConstructor
/**
 * Exposes REST endpoints for landing page data and initialization.
 * Returns memberships, benefits, tagline, and basic information.
 */
public class LandingPageApiController {

    private final LandingPageApiService landingPageApiService;

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<LandingPageDataResponse>> getLandingPageData() {
        return ResponseEntity.ok(ApiResponse.success("Landing page data fetched", landingPageApiService.getLandingPageData()));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> health() {
        return ResponseEntity.ok(ApiResponse.success("Landing page service is running", null));
    }

    @GetMapping("/initialize")
    public ResponseEntity<ApiResponse<Void>> initialize() {
        landingPageApiService.initializeDefaults();
        return ResponseEntity.ok(ApiResponse.success("Default landing page content initialized successfully", null));
    }
}
