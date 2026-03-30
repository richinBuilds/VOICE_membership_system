package org.voice.membership.services;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.MultiStepRegistrationDto;
import org.voice.membership.entities.Membership;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationStep4Service {

    private final MembershipService membershipService;

    public Optional<Membership> resolveMembership(HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null || registrationData.getCartMembershipId() == null) {
            return Optional.empty();
        }
        return membershipService.getMembershipById(registrationData.getCartMembershipId());
    }

    public String handleStep4Action(String action, HttpSession session, RegistrationCompletionService completionService) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        if ("remove".equals(action)) {
            registrationData.setCartMembershipId(null);
            registrationData.setSelectedMembershipId(null);
            session.setAttribute("registrationData", registrationData);
            return "redirect:/register/step3";
        }

        if (registrationData.getCartMembershipId() == null) {
            return "redirect:/register/step3";
        }

        Optional<Membership> membershipOpt = membershipService.getMembershipById(registrationData.getCartMembershipId());
        if (membershipOpt.isEmpty()) {
            return "redirect:/register/step3";
        }

        Membership membership = membershipOpt.get();
        return membership.isFree()
                ? completionService.completeRegistration(session)
                : "redirect:/register/checkout";
    }
}
