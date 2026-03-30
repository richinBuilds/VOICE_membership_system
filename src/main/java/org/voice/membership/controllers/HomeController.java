package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.voice.membership.dtos.HomePageViewData;
import org.voice.membership.services.HomePageService;
import java.security.Principal;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
/**
 * Handles the public home and login pages for the site.
 * Populates basic landing content and login state for the index view.
 */
public class HomeController {

    private final HomePageService homePageService;

    @GetMapping
    public String index(Model model, Principal principal) {
        HomePageViewData viewData = homePageService.getHomePageViewData();
        model.addAttribute("heroTitle", viewData.heroTitle());
        model.addAttribute("heroTagline", viewData.heroTagline());
        model.addAttribute("benefitsTitle", viewData.benefitsTitle());
        model.addAttribute("reasonsHeading", viewData.reasonsHeading());
        model.addAttribute("reasonsContent", viewData.reasonsContent());
        model.addAttribute("memberships", viewData.memberships());
        model.addAttribute("lineSeparator", System.lineSeparator());
        model.addAttribute("isUserLoggedIn", viewData.isUserLoggedIn());

        return "index";
    }

    @GetMapping("login")
    public String login() {
        return "login";
    }
}
