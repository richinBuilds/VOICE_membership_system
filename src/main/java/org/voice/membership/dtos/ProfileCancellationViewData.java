package org.voice.membership.dtos;

import org.voice.membership.entities.User;

public record ProfileCancellationViewData(
        User user,
        String userName,
        String currentMembershipName,
        boolean isFree) {
}
