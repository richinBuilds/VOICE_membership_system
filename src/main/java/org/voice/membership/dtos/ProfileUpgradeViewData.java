package org.voice.membership.dtos;

import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;

import java.util.List;

public record ProfileUpgradeViewData(
        User user,
        Membership currentMembership,
        List<Membership> paidMemberships,
        String userName) {
}
