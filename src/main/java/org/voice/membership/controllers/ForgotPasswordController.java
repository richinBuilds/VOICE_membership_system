package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import org.voice.membership.dtos.ForgotPasswordRequest;
import org.voice.membership.services.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor

public class ForgotPasswordController {

    private final UserService userService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute ForgotPasswordRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("message", "Please provide a valid email address.");
            return "forgot-password";
        }
        boolean result = userService.sendPasswordResetEmail(request.email());
        if (result) {
            model.addAttribute("message", "A password reset link has been sent to your email address.");
        } else {
            model.addAttribute("message", "No account found with that email address.");
        }
        return "forgot-password";
    }
}
