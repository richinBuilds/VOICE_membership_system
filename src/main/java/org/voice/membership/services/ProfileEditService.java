package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.UpdateUserRequest;
import org.voice.membership.entities.User;

@Service
@RequiredArgsConstructor
public class ProfileEditService {

    private final CurrentUserService currentUserService;
    private final UserService userService;

    public UpdateUserRequest buildUpdateRequest(String email) {
        User user = currentUserService.getCurrentUser(email);
        if (user == null) {
            return null;
        }

        return UpdateUserRequest.builder()
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .province(user.getProvince())
                .postalCode(user.getPostalCode())
                .build();
    }

    public User updateProfile(String currentEmail, UpdateUserRequest updateUserRequest) {
        User updatedUser = userService.updateProfile(currentEmail, updateUserRequest);
        if (updatedUser != null) {
            refreshAuthenticationIfEmailChanged(currentEmail, updateUserRequest.getEmail());
        }
        return updatedUser;
    }

    private void refreshAuthenticationIfEmailChanged(String oldEmail, String newEmail) {
        if (newEmail == null || newEmail.equalsIgnoreCase(oldEmail)) {
            return;
        }

        try {
            UserDetails newDetails = userService.loadUserByUsername(newEmail);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    newDetails,
                    newDetails.getPassword(),
                    newDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        } catch (Exception ignored) {
        }
    }
}
