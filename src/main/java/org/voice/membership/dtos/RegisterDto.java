package org.voice.membership.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.voice.membership.validation.StrongPassword;

@Data
public class RegisterDto {

    @NotEmpty
    private String name;

    @NotEmpty
    @Email
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address (e.g., name@example.com)")
    private String email;

    @StrongPassword
    private String password;
    private String confirmPassword;
    @NotEmpty(message = "Phone number is required")
    @Pattern(regexp = "^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$", message = "Invalid Canadian phone number format. Use: (XXX) XXX-XXXX or XXX-XXX-XXXX or XXXXXXXXXX (10 digits)")
    private String phone;
    private String address;
    @Pattern(regexp = "^[A-Za-z][0-9][A-Za-z][ ]?[0-9][A-Za-z][0-9]$", message = "Valid Canadian postal code, e.g., A1A 1A1")
    private String postalCode;
}