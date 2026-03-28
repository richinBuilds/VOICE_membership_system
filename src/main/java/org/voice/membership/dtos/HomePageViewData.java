package org.voice.membership.dtos;

import lombok.Builder;
import org.voice.membership.entities.Membership;

import java.util.List;

@Builder
public record HomePageViewData(
        String heroTitle,
        String heroTagline,
        String benefitsTitle,
        String reasonsHeading,
        String reasonsContent,
        List<Membership> memberships,
        String isUserLoggedIn) {
}
