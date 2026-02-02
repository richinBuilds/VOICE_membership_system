package org.voice.membership.controllers;

import org.voice.membership.services.UserService;
import org.voice.membership.validation.PasswordPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
/**
 * Handles the password reset form after a reset token has been issued.
 * Validates new passwords and delegates the actual reset to UserService.
 */
public class ResetPasswordController {

    @Autowired
    private UserService userService;

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        if (!PasswordPolicy.isStrong(password)) {
            model.addAttribute("message",
                    "Password must be 8-64 chars, include upper, lower, number, special, and contain no spaces.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("message", "Passwords do not match.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        boolean result = userService.resetPassword(token, password);
        if (result) {
            model.addAttribute("message", "Your password has been reset. You can now log in.");
        } else {
            model.addAttribute("message", "Invalid or expired reset link.");
        }
        return "reset-password";
    }
}
