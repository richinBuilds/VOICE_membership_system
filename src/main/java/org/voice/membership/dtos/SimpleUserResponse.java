package org.voice.membership.dtos;

import lombok.Builder;

import java.util.Date;

@Builder
public record SimpleUserResponse(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String membershipName,
        Date registrationDate) {
}
