package org.voice.membership.dtos;

import lombok.Builder;
import org.voice.membership.entities.Membership;

import java.util.List;

@Builder
public record LandingPageDataResponse(String tagline, List<Membership> memberships, String isUserLoggedIn) {
}
