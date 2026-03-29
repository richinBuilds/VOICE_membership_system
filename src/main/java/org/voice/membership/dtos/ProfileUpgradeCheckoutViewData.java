package org.voice.membership.dtos;

import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;

public record ProfileUpgradeCheckoutViewData(
        User user,
        Membership currentMembership,
        Membership upgradeMembership,
        String membershipName,
        Object membershipPrice,
        String userName,
        String paypalClientId,
        String paypalCurrency,
        String mode) {
}
