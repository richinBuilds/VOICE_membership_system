package org.voice.membership.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.voice.membership.services.LandingPageService;
import org.springframework.beans.factory.annotation.Autowired;
import java.security.Principal;

@Controller
@RequestMapping("/")
/**
 * Handles the public home and login pages for the site.
 * Populates basic landing content and login state for the index view.
 */
public class HomeController {

    @Autowired
    private LandingPageService landingPageService;

    @GetMapping
    public String index(Model model, Principal principal) {
        model.addAttribute("heroTitle", landingPageService.getHeroTitle());
        model.addAttribute("heroTagline", landingPageService.getHeroTagline());
        model.addAttribute("benefitsTitle", landingPageService.getBenefitsTitle());
        model.addAttribute("reasonsHeading", landingPageService.getReasonsHeading());
        model.addAttribute("reasonsContent", landingPageService.getReasonsContent());
        model.addAttribute("memberships", landingPageService.getActiveMemberships());
        model.addAttribute("lineSeparator", System.lineSeparator());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() &&
                !auth.getPrincipal().equals("anonymousUser");
        model.addAttribute("isUserLoggedIn", String.valueOf(isAuthenticated));

        return "index";
    }

    @GetMapping("login")
    public String login() {
        return "login";
    }
}
