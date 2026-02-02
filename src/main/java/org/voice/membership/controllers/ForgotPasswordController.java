package org.voice.membership.controllers;

import org.voice.membership.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
/**
 * Manages the "forgot password" flow before a reset token is issued.
 * Shows the request form and triggers sending of password reset emails.
 */
public class ForgotPasswordController {

    @Autowired
    private UserService userService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        boolean result = userService.sendPasswordResetEmail(email);
        if (result) {
            model.addAttribute("message", "A password reset link has been sent to your email address.");
        } else {
            model.addAttribute("message", "No account found with that email address.");
        }
        return "forgot-password";
    }
}
