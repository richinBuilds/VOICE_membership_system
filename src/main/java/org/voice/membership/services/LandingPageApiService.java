package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.LandingPageDataResponse;

@Service
@RequiredArgsConstructor
public class LandingPageApiService {

    private final LandingPageService landingPageService;

    public LandingPageDataResponse getLandingPageData() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal());

        return LandingPageDataResponse.builder()
                .tagline(landingPageService.getTagline())
                .memberships(landingPageService.getActiveMemberships())
                .isUserLoggedIn(String.valueOf(isAuthenticated))
                .build();
    }

    public void initializeDefaults() {
        landingPageService.initializeDefaultContent();
        landingPageService.initializeDefaultMemberships();
    }
}
