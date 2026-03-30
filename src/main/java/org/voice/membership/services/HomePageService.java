package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.HomePageViewData;

@Service
@RequiredArgsConstructor
public class HomePageService {

    private final LandingPageService landingPageService;

    public HomePageViewData getHomePageViewData() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal());

        return HomePageViewData.builder()
                .heroTitle(landingPageService.getHeroTitle())
                .heroTagline(landingPageService.getHeroTagline())
                .benefitsTitle(landingPageService.getBenefitsTitle())
                .reasonsHeading(landingPageService.getReasonsHeading())
                .reasonsContent(landingPageService.getReasonsContent())
                .memberships(landingPageService.getActiveMemberships())
                .isUserLoggedIn(String.valueOf(isAuthenticated))
                .build();
    }
}
