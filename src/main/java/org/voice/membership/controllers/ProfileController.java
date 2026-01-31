
package org.voice.membership.controllers;

import org.voice.membership.dtos.UpdateUserRequest;
import org.voice.membership.entities.User;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Child;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.ChildRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
/**

 Handles user dashboard and profile management.
 Manages user profile viewing, editing, and child account operations.
 Displays user membership information, manages child information (add, edit, delete).
 Provides endpoints for profile updates and child management on the user dashboard.
 */
@Controller
@AllArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ChildRepository childRepository;
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

            // Get children information
            List<Child> children = childRepository.findByUser(user);
            model.addAttribute("children", children);

            // Get membership information
            Membership membership = user.getMembership();
            if (membership != null) {
                model.addAttribute("membershipType", membership.getName());
                
                // For paid members, set status to "Paid" and calculate expiry date (1 year from creation)
                if (!membership.isFree()) {
                    model.addAttribute("membershipStatus", "Paid");
                    
                    // Calculate expiry date: 1 year from user creation date
                    if (user.getCreation() != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(user.getCreation());
                        cal.add(Calendar.YEAR, 1);
                        Date expiryDate = cal.getTime();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                        model.addAttribute("membershipExpiryDate", dateFormat.format(expiryDate));
                    } else {
                        model.addAttribute("membershipExpiryDate", "-");
                    }
                    
                    // Don't show benefits for paid members
                    model.addAttribute("showBenefits", false);
                } else {
                    // Free membership
                    model.addAttribute("membershipStatus", "Free");
                    model.addAttribute("membershipExpiryDate", "No expiry");
                    model.addAttribute("showBenefits", true);
                    model.addAttribute("membershipBenefit", membership.getDescription() != null ? membership.getDescription() : "-");
                }
            } else {
                // No membership assigned
                model.addAttribute("membershipStatus", "None");
                model.addAttribute("membershipType", "No Membership Yet");
                model.addAttribute("membershipExpiryDate", "-");
                model.addAttribute("showBenefits", false);
            }

            return "profile";
        } catch (Exception e) {
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
            model.addAttribute("updateUserRequest", updateUserRequest);
            return "editProfile";
        }
    }

    // Child Management
    @GetMapping("/child/add")
    public String addChild(Model model, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }
            // Create a new empty child object
            Child newChild = new Child();
            model.addAttribute("child", newChild);
            model.addAttribute("isEdit", false);
            return "editChild";
        } catch (Exception e) {
            System.err.println("Error loading add child form: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/profile?error=add_child_failed";
        }
    }

    @PostMapping("/child/add")
    public String saveChild(@RequestParam("name") String name,
                           @RequestParam(value = "age", required = false) Integer age,
                           @RequestParam(value = "dateOfBirth", required = false) String dateOfBirthStr,
                           @RequestParam(value = "hearingLossType", required = false) String hearingLossType,
                           @RequestParam(value = "equipmentType", required = false) String equipmentType,
                           @RequestParam(value = "siblingsNames", required = false) String siblingsNames,
                           @RequestParam(value = "chapterLocation", required = false) String chapterLocation,
                           Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            Child child = Child.builder()
                    .name(name)
                    .age(age)
                    .hearingLossType(hearingLossType)
                    .equipmentType(equipmentType)
                    .siblingsNames(siblingsNames)
                    .chapterLocation(chapterLocation)
                    .user(user)
                    .build();

            if (dateOfBirthStr != null && !dateOfBirthStr.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    child.setDateOfBirth(sdf.parse(dateOfBirthStr));
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }

            childRepository.save(child);
            return "redirect:/profile";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile/child/add?error=save_failed";
        }
    }

    @GetMapping("/child/edit/{id}")
    public String editChild(@PathVariable("id") int id, Model model, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            Optional<Child> childOpt = childRepository.findById(id);
            if (childOpt.isEmpty() || childOpt.get().getUser().getId() != user.getId()) {
                return "redirect:/profile";
            }

            model.addAttribute("child", childOpt.get());
            model.addAttribute("isEdit", true);
            return "editChild";
        } catch (Exception e) {
            System.err.println("Error loading edit child form: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/profile?error=edit_child_failed";
        }
    }

    @PostMapping("/child/edit/{id}")
    public String updateChild(@PathVariable("id") int id,
                            @RequestParam("name") String name,
                            @RequestParam(value = "age", required = false) Integer age,
                            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirthStr,
                            @RequestParam(value = "hearingLossType", required = false) String hearingLossType,
                            @RequestParam(value = "equipmentType", required = false) String equipmentType,
                            @RequestParam(value = "siblingsNames", required = false) String siblingsNames,
                            @RequestParam(value = "chapterLocation", required = false) String chapterLocation,
                            Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            Optional<Child> childOpt = childRepository.findById(id);
            if (childOpt.isEmpty() || childOpt.get().getUser().getId() != user.getId()) {
                return "redirect:/profile";
            }

            Child child = childOpt.get();
            child.setName(name);
            child.setAge(age);
            child.setHearingLossType(hearingLossType);
            child.setEquipmentType(equipmentType);
            child.setSiblingsNames(siblingsNames);
            child.setChapterLocation(chapterLocation);

            if (dateOfBirthStr != null && !dateOfBirthStr.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    child.setDateOfBirth(sdf.parse(dateOfBirthStr));
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }

            childRepository.save(child);
            return "redirect:/profile";
        } catch (Exception e) {

            e.printStackTrace();
            return "redirect:/profile/child/edit/" + id + "?error=update_failed";
        }
    }

    @PostMapping("/child/delete/{id}")
    public String deleteChild(@PathVariable("id") int id, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            Optional<Child> childOpt = childRepository.findById(id);
            if (childOpt.isPresent() && childOpt.get().getUser().getId() == user.getId()) {
                childRepository.delete(childOpt.get());
            }
            return "redirect:/profile";
        } catch (Exception e) {
            System.out.println("Error deleting child: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/profile";
        }
    }

    // Upgrade Membership
    @GetMapping("/upgrade-membership")
    public String upgradeMembershipPage(Model model, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            // Check if user has free membership
            Membership membership = user.getMembership();
            if (membership == null || !membership.isFree()) {
                // User is already paid or has no membership, redirect to profile
                return "redirect:/profile?error=not_eligible_for_upgrade";
            }

            // Get all paid membership options
            List<Membership> paidMemberships = membershipRepository.findByIsFree(false);
            
            model.addAttribute("user", user);
            model.addAttribute("currentMembership", membership);
            model.addAttribute("paidMemberships", paidMemberships);
            model.addAttribute("userName", user.getName());

            return "upgrade-membership";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile?error=upgrade_load_failed";
        }
    }

    @PostMapping("/upgrade-membership/select")
    public String selectUpgradeMembership(@RequestParam("membershipId") Integer membershipId, 
                                         Model model,
                                         Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            // Verify user has free membership
            Membership currentMembership = user.getMembership();
            if (currentMembership == null || !currentMembership.isFree()) {
                return "redirect:/profile?error=not_eligible_for_upgrade";
            }

            // Get the paid membership
            Optional<Membership> paidMembershipOpt = membershipRepository.findById(membershipId);
            if (paidMembershipOpt.isEmpty() || paidMembershipOpt.get().isFree()) {
                return "redirect:/profile/upgrade-membership?error=invalid_membership";
            }

            Membership paidMembership = paidMembershipOpt.get();
            
            // Store in session or model for checkout
            model.addAttribute("user", user);
            model.addAttribute("upgradeMembership", paidMembership);
            model.addAttribute("userName", user.getName());
            model.addAttribute("membershipName", paidMembership.getName());
            model.addAttribute("membershipPrice", paidMembership.getPrice());
            model.addAttribute("membershipDescription", paidMembership.getDescription());

            return "upgrade-checkout";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile/upgrade-membership?error=selection_failed";
        }
    }
}
