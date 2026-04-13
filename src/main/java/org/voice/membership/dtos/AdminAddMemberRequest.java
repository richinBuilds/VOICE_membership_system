package org.voice.membership.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for admin to manually add a new member to the system.
 * Used for handling exceptional cases or administrative corrections.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAddMemberRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9\\-\\+\\(\\)\\s]+$", message = "Please provide a valid phone number")
    private String phone;

    private String address;

    private String city;

    private String province;

    private String postalCode;

    private String chapter;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$", message = "Password must be at least 8 characters with uppercase, lowercase, number, and special character")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    private Integer membershipId;

    @NotNull(message = "Email verification status is required")
    private Boolean emailVerified;

    @NotNull(message = "Account lock status is required")
    private Boolean accountLocked;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
