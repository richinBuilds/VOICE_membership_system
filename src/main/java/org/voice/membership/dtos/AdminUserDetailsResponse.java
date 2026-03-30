package org.voice.membership.dtos;

import lombok.Builder;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.Membership;

import java.util.Date;
import java.util.List;

@Builder
public record AdminUserDetailsResponse(
        Integer id,
        String firstName,
        String middleName,
        String lastName,
        String email,
        String phone,
        String address,
        String postalCode,
        String role,
        Date creation,
        List<Child> children,
        Membership membership) {
}
