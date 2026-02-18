package org.voice.membership.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for admin to update user profile information.
 * Contains all user fields that an admin can modify, including membership assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {
    
    @NotNull(message = "User ID is required")
    private Integer userId;
    
    @NotEmpty(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotEmpty(message = "Last name is required")
    private String lastName;

    @NotEmpty(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", 
             message = "Please enter a valid email address (e.g., name@example.com)")
    private String email;

    @NotEmpty(message = "Phone number is required")
    private String phone;

    private String address;

    private String city;

    private String province;

    @Pattern(regexp = "^[A-Za-z][0-9][A-Za-z][ ]?[0-9][A-Za-z][0-9]$", 
             message = "Valid Canadian postal code, e.g., A1A 1A1")
    private String postalCode;
    
    // Membership information (admin can change membership)
    private Integer membershipId;
    
    // Email verification status (admin can toggle this)
    private Boolean emailVerified;
    
    // Account locked status (admin can unlock accounts)
    private Boolean accountLocked;
}
