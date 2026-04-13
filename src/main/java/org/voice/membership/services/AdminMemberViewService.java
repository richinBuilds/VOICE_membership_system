package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.AdminUpdateUserRequest;
import org.voice.membership.dtos.AdminUserDetailsResponse;
import org.voice.membership.entities.User;

@Service
@RequiredArgsConstructor
public class AdminMemberViewService {

    public AdminUserDetailsResponse buildUserDetails(User user) {
        return AdminUserDetailsResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .province(user.getProvince())
                .postalCode(user.getPostalCode())
                .chapter(user.getChapter())
                .role(user.getRole() != null ? user.getRole() : "USER")
                .creation(user.getCreation())
                .paid(user.isPaid())
                .emailVerified(user.isEmailVerified())
                .accountLocked(user.isAccountLocked())
                .membershipStartDate(user.getMembershipStartDate())
                .membershipExpiryDate(user.getMembershipExpiryDate())
                .children(user.getChildren())
                .membership(user.getMembership())
                .build();
    }

    public AdminUpdateUserRequest buildUpdateRequest(User user) {
        return AdminUpdateUserRequest.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .province(user.getProvince())
                .postalCode(user.getPostalCode())
                .chapter(user.getChapter())
                .membershipId(user.getMembership() != null ? user.getMembership().getId() : null)
                .emailVerified(user.isEmailVerified())
                .accountLocked(user.isAccountLocked())
                .build();
    }
}
