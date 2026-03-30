package org.voice.membership.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.voice.membership.controllers.AdminController;
import org.voice.membership.controllers.AdminContentController;
import org.voice.membership.controllers.AdminMemberManagementController;
import org.voice.membership.controllers.AdminRenewalController;
import org.voice.membership.services.AdminMemberService;

@ControllerAdvice(assignableTypes = {
    AdminController.class,
    AdminMemberManagementController.class,
    AdminContentController.class,
    AdminRenewalController.class
})
@RequiredArgsConstructor
public class AdminModelAttributeAdvice {

    private final AdminMemberService adminMemberService;

    @ModelAttribute
    public void addAdminAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return;
        }

        String adminEmail = auth.getName();
        model.addAttribute("adminEmail", adminEmail);
        model.addAttribute("adminName", adminMemberService.getAdminNameByEmail(adminEmail));
    }
}
