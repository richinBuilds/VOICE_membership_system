package org.voice.membership.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.voice.membership.dtos.MessageViewData;
import org.voice.membership.dtos.ResendVerificationRequest;
import org.voice.membership.services.RegistrationVerificationViewService;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterVerificationController {

    private final RegistrationVerificationViewService registrationVerificationViewService;

    @GetMapping("/verification-sent")
    public String showVerificationSent(Model model) {
        model.addAttribute("message", "Registration successful! Please check your email to verify your account.");
        return "verification-sent";
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        MessageViewData messageViewData = registrationVerificationViewService.verify(token);
        model.addAttribute("success", messageViewData.success());
        model.addAttribute("error", messageViewData.error());
        return "verification-result";
    }

    @GetMapping("/resend-verification")
    public String showResendVerification() {
        return "resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(
            @Valid @ModelAttribute ResendVerificationRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please provide a valid email address.");
            return "resend-verification";
        }

        MessageViewData messageViewData = registrationVerificationViewService.resend(request.email());
        model.addAttribute("success", messageViewData.success());
        model.addAttribute("error", messageViewData.error());
        return "resend-verification";
    }
}
