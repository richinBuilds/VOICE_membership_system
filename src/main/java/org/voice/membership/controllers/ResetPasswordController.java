package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import org.voice.membership.dtos.ResetPasswordRequest;
import org.voice.membership.services.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
/**
 * Handles the password reset form after a reset token has been issued.
 * Validates new passwords and delegates the actual reset to UserService.
 */
public class ResetPasswordController {

    private final UserService userService;

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute ResetPasswordRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("message", "Please fix the password validation errors.");
            model.addAttribute("token", request.token());
            return "reset-password";
        }
        boolean result = userService.resetPassword(request.token(), request.password());
        if (result) {
            model.addAttribute("message", "Your password has been reset. You can now log in.");
        } else {
            model.addAttribute("message", "Invalid or expired reset link.");
        }
        model.addAttribute("token", request.token());
        return "reset-password";
    }
}
