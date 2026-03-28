package org.voice.membership.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.voice.membership.config.PayPalProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.voice.membership.dtos.*;
import org.voice.membership.entities.Membership;
import org.voice.membership.services.GoogleOAuth2UserService;
import org.voice.membership.services.MembershipService;
import org.voice.membership.services.RegistrationCheckoutService;
import org.voice.membership.services.RegistrationCompletionService;
import org.voice.membership.services.RegistrationStep1Service;
import org.voice.membership.services.RegistrationStep4Service;
import org.voice.membership.services.RegistrationStep2Service;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
/**
 * Implements the multi-step user registration, child details, and membership
 * checkout flow.
 * Guides users through all registration steps. Delegates all business logic
 * (user creation, verification, cart, payments) to {@link RegistrationService}.
 */
public class RegisterController {

    private final MembershipService membershipService;
    private final PayPalProperties payPalProperties;
    private final RegistrationCheckoutService registrationCheckoutService;
    private final RegistrationCompletionService registrationCompletionService;
    private final RegistrationStep2Service registrationStep2Service;
    private final RegistrationStep1Service registrationStep1Service;
    private final RegistrationStep4Service registrationStep4Service;

    @GetMapping
    public String showRegister(Model model, HttpSession session) {
        session.removeAttribute("registrationData");
        model.addAttribute("registerDto", new RegisterDto());
        model.addAttribute("step", 1);
        model.addAttribute("totalSteps", 4);
        return "register";
    }

    @GetMapping("/google")
    public String startGoogleSignup(HttpSession session) {
        session.setAttribute(GoogleOAuth2UserService.GOOGLE_AUTH_FLOW_SESSION_KEY,
                GoogleOAuth2UserService.GOOGLE_AUTH_FLOW_SIGNUP);
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/step1")
    public String handleStep1(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
            BindingResult bindingResult,
            Model model,
            HttpSession session) {
        registrationStep1Service.validate(registerDto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("registerDto", registerDto);
            model.addAttribute("step", 1);
            model.addAttribute("totalSteps", 4);
            return "register";
        }

        registrationStep1Service.storeRegistration(session, registerDto);

        return "redirect:/register/step2";
    }

    @GetMapping("/step2")
    public String showStep2(Model model, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        registrationStep2Service.initializeChildrenIfNeeded(registrationData);

        model.addAttribute("children", registrationData.getChildren());
        model.addAttribute("step", 2);
        model.addAttribute("totalSteps", 4);
        return "register-step2";
    }

    @PostMapping("/step2")
    public String handleStep2(@ModelAttribute RegisterStep2Request request, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        if (registrationStep2Service.isAddChildAction(request)) {
            registrationStep2Service.addEmptyChild(registrationData);
            session.setAttribute("registrationData", registrationData);
            return "redirect:/register/step2";
        }

        registrationData.setChildren(registrationStep2Service.mapChildren(request));
        session.setAttribute("registrationData", registrationData);

        return "redirect:/register/step3";
    }

    @GetMapping("/step3")
    public String showStep3(Model model, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        List<Membership> memberships = membershipService.getActiveMembershipsOrdered();
        model.addAttribute("memberships", memberships);
        model.addAttribute("selectedMembershipId", registrationData.getSelectedMembershipId());
        model.addAttribute("lineSeparator", System.lineSeparator());
        model.addAttribute("step", 3);
        model.addAttribute("totalSteps", 4);
        return "register-step3";
    }

    @PostMapping("/step3")
    public String handleStep3(@Valid @ModelAttribute MembershipSelectionRequest request,
            BindingResult bindingResult,
            HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/register/step3";
        }

        registrationData.setSelectedMembershipId(request.membershipId());
        registrationData.setCartMembershipId(request.membershipId());
        session.setAttribute("registrationData", registrationData);
        return "redirect:/register/step4";
    }

    @GetMapping("/step4")
    public String showStep4(@RequestParam(value = "error", required = false) String error,
            Model model, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        if (registrationData.getCartMembershipId() == null) {
            return "redirect:/register/step3";
        }

        Optional<Membership> membershipOpt = registrationStep4Service.resolveMembership(session);
        if (membershipOpt.isEmpty()) {
            return "redirect:/register/step3";
        }

        model.addAttribute("membership", membershipOpt.get());
        model.addAttribute("lineSeparator", System.lineSeparator());
        model.addAttribute("step", 4);
        model.addAttribute("totalSteps", 4);
        if (error != null) {
            model.addAttribute("error", "An error occurred. Please try again.");
        }
        return "register-step4";
    }

    @PostMapping("/step4")
    public String handleStep4(@RequestParam(value = "action", required = false) String action,
            HttpSession session) {
        try {
            return registrationStep4Service.handleStep4Action(action, session, registrationCompletionService);
        } catch (Exception e) {
            return "redirect:/register/step4?error=processing_failed";
        }
    }

    @GetMapping("/checkout")
    public String showCheckout(Model model, HttpSession session) {
        Optional<Membership> membershipOpt = registrationCheckoutService.resolveCheckoutMembership(session);
        if (membershipOpt.isEmpty()) {
            return "redirect:/register/step3";
        }

        Membership membership = membershipOpt.get();
        if (membership.isFree()) {
            return registrationCompletionService.completeRegistration(session);
        }

        model.addAttribute("membership", membership);
        model.addAttribute("totalAmount", membership.getPrice());
        model.addAttribute("paypalClientId", payPalProperties.getClientId());
        model.addAttribute("paypalCurrency", payPalProperties.getCurrency());
        model.addAttribute("mode", "registration");
        return "checkout";
    }

    @PostMapping("/checkout")
    public String handleCheckout(HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }
        return "redirect:/register/checkout?error=use_paypal_checkout";
    }

    @PostMapping("/paypal/checkout/create-order")
    @ResponseBody
    public ResponseEntity<ApiResponse<PayPalOrderResponse>> createCheckoutOrder(
            @Valid @RequestBody CreatePayPalOrderRequest request,
            HttpSession session) {
        PayPalOrderResponse response = registrationCheckoutService.createOrder(request.membershipId(), session);
        return ResponseEntity.ok(ApiResponse.success("Order created", response));
    }

    @PostMapping("/paypal/checkout/capture-order")
    @ResponseBody
    public ResponseEntity<ApiResponse<RedirectResponse>> captureCheckoutOrder(
            @Valid @RequestBody CapturePayPalOrderRequest request,
            HttpSession session) {
        RedirectResponse response = registrationCheckoutService.captureOrder(request, session, registrationCompletionService);
        return ResponseEntity.ok(ApiResponse.success("Payment finalized", response));
    }

}
