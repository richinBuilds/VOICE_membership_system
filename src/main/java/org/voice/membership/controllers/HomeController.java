package org.voice.membership.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Arrays;
import java.security.Principal;

@Controller
@RequestMapping("/")
/**
 * Handles the public home and login pages for the site.
 * Populates basic landing content and login state for the index view.
 */
public class HomeController {

    @GetMapping
    public String index(Model model, Principal principal) {
        model.addAttribute("tagline", "Empowering families of children who are Deaf and Hard of Hearing");
        model.addAttribute("benefits", Arrays.asList());
        model.addAttribute("memberships", Arrays.asList());

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
