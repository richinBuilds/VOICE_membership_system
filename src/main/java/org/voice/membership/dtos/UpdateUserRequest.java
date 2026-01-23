package org.voice.membership.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
    @NotEmpty
    private String name;

    @NotEmpty
    @Email
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address (e.g., name@example.com)")
    private String email;

    @NotEmpty(message = "Phone number is required")
    private String phone;

    private String address;

    @Pattern(regexp = "^[ABCEGHJ-NPRSTVXYabceghj-nprstvxy][0-9][ABCEGHJ-NPRSTVXYabceghj-nprstvxy][ ]?[0-9][ABCEGHJ-NPRSTVXYabceghj-nprstvxy][0-9]$", message = "Valid Canadian postal code, e.g., A1A 1A1")
    private String postalCode;
}