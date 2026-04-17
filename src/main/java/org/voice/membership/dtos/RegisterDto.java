package org.voice.membership.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.voice.membership.validation.StrongPassword;

@Data
/**
 * Carries user registration details from the signup form.
 * Includes basic contact info and strong password validation constraints.
 */
public class RegisterDto {

    @NotEmpty(message = "First name is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z0-9\\s'-]+$", message = "First name must include at least one letter and may contain letters, numbers, spaces, apostrophes, or hyphens")
    private String firstName;

    private String middleName;

    @NotEmpty(message = "Last name is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z0-9\\s'-]+$", message = "Last name must include at least one letter and may contain letters, numbers, spaces, apostrophes, or hyphens")
    private String lastName;

    @NotEmpty
    @Email
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address (e.g., name@example.com)")
    private String email;

    @StrongPassword
    private String password;

    @NotEmpty(message = "Confirm password is required")
    private String confirmPassword;
    @NotEmpty(message = "Phone number is required")
    @Pattern(regexp = "^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$", message = "Invalid Canadian phone number format. Use: (XXX) XXX-XXXX or XXX-XXX-XXXX or XXXXXXXXXX (10 digits)")
    private String phone;
    private String address;
    private String city;
    private String province;
    @Pattern(regexp = "^$|^[A-Za-z][0-9][A-Za-z][ ]?[0-9][A-Za-z][0-9]$", message = "Valid Canadian postal code, e.g., A1A 1A1")
    private String postalCode;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatching() {
        return password == null || confirmPassword == null || password.equals(confirmPassword);
    }
}
