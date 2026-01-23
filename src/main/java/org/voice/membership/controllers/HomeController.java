package org.voice.membership.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.security.Principal;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String index(Model model, Principal principal) {
        // Add simple VOICE landing page information
        model.addAttribute("tagline", "Empowering families of children who are Deaf and Hard of Hearing");

        // Add empty collections for benefits and memberships to prevent template errors
        model.addAttribute("benefits", Arrays.asList());
        model.addAttribute("memberships", Arrays.asList());

        // Check if user is authenticated (including via remember-me)
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
