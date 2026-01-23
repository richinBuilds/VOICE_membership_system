package org.voice.membership.controllers;

import org.voice.membership.dtos.UpdateUserRequest;
import org.voice.membership.entities.User;
import org.voice.membership.entities.Membership;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.services.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final UserService userService;

    @GetMapping
    public String profile(Model model, Principal principal) {
        try {
            // Get the logged-in user's information
            User user = userRepository.findByEmail(principal.getName());

            if (user == null) {
                return "redirect:/login";
            }

            // Add user information to model
            model.addAttribute("user", user);
            model.addAttribute("userName", user.getName());
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("userPhone", user.getPhone() != null ? user.getPhone() : "Not provided");
            model.addAttribute("userAddress", user.getAddress() != null ? user.getAddress() : "Not provided");
            model.addAttribute("userPostalCode", user.getPostalCode() != null ? user.getPostalCode() : "Not provided");

            // Format creation date
            if (user.getCreation() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                model.addAttribute("memberSince", dateFormat.format(user.getCreation()));
            } else {
                model.addAttribute("memberSince", "Recently");
            }

            // Get all membership options for display
            List<Membership> memberships = membershipRepository.findAll();
            model.addAttribute("memberships", memberships);

            // Default: no membership assigned yet
            model.addAttribute("membershipStatus", "None");
            model.addAttribute("membershipType", "No Membership Yet");
            model.addAttribute("membershipExpiryDate", "-");
            model.addAttribute("membershipBenefit", "-");

            return "profile";
        } catch (Exception e) {
            System.out.println("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login";
        }
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName());
        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .postalCode(user.getPostalCode())
                .build();
        model.addAttribute("updateUserRequest", updateUserRequest);
        return "editProfile";
    }

    @PostMapping("/edit")
    public String editProfile(Model model,
            @Valid @ModelAttribute("updateUserRequest") UpdateUserRequest updateUserRequest,
            BindingResult bindingResult,
            Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (bindingResult.hasErrors()) {
                model.addAttribute("updateUserRequest", updateUserRequest);
                return "editProfile";
            }

            user.setName(updateUserRequest.getName());
            // If email changed, update and refresh session principal
            String oldEmail = user.getEmail();
            String newEmail = updateUserRequest.getEmail();
            if (newEmail != null && !newEmail.equalsIgnoreCase(oldEmail)) {
                // Validate uniqueness: any other account with same email (case-insensitive)
                List<User> matches = userRepository.findAllByEmailIgnoreCase(newEmail);
                boolean conflict = matches.stream().anyMatch(u -> u.getId() != user.getId());
                if (conflict) {
                    bindingResult.addError(new FieldError(
                            "updateUserRequest", "email", "email already exist. choose different"));
                    model.addAttribute("updateUserRequest", updateUserRequest);
                    return "editProfile";
                }
                user.setEmail(newEmail);
            }
            user.setPhone(updateUserRequest.getPhone());
            user.setAddress(updateUserRequest.getAddress());
            user.setPostalCode(updateUserRequest.getPostalCode());
            userRepository.save(user);
            if (newEmail != null && !newEmail.equalsIgnoreCase(oldEmail)) {
                try {
                    UserDetails newDetails = userService.loadUserByUsername(newEmail);
                    Authentication newAuth = new UsernamePasswordAuthenticationToken(newDetails,
                            newDetails.getPassword(), newDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                } catch (Exception ex) {
                    // If re-auth fails, user can re-login manually
                }
            }
            return "redirect:/profile";
        } catch (Exception e) {
            System.out.println("Error in edit profile: " + e.getMessage());
            model.addAttribute("updateUserRequest", updateUserRequest);
            return "editProfile";
        }
    }
}
