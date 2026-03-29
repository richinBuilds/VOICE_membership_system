package org.voice.membership.dtos;

import lombok.Builder;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;

import java.util.List;

@Builder
public record ProfilePageViewData(
        User user,
        String userName,
        String userEmail,
        String userPhone,
        String userAddress,
        String userCity,
        String userProvince,
        String userPostalCode,
        String memberSince,
        List<Child> children,
        String membershipStatus,
        String membershipType,
        boolean hasPaidMembership,
        String membershipExpiryDate,
        boolean showBenefits,
        boolean isMembershipExpired,
        String membershipBenefit) {
}
