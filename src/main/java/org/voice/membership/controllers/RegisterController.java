package org.voice.membership.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.voice.membership.dtos.*;
import org.voice.membership.entities.*;
import org.voice.membership.repositories.*;
import org.voice.membership.services.UserService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/register")
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserService userService;

    // Step 1: User Details
    @GetMapping
    public String showRegister(Model model, HttpSession session) {
        // Clear any existing session data
        session.removeAttribute("registrationData");
        model.addAttribute("registerDto", new RegisterDto());
        model.addAttribute("step", 1);
        model.addAttribute("totalSteps", 4);
        return "register";
    }

    @PostMapping("/step1")
    public String handleStep1(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
                              BindingResult bindingResult,
                              Model model,
                              HttpSession session) {
        // Confirm password match
        if (registerDto.getPassword() != null && registerDto.getConfirmPassword() != null
                && !registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            bindingResult.addError(new FieldError("registerDto", "confirmPassword", "Passwords do not match"));
        }

        // Email uniqueness check (case-insensitive)
        if (registerDto.getEmail() != null) {
            List<User> matches = userRepository.findAllByEmailIgnoreCase(registerDto.getEmail());
            if (!matches.isEmpty()) {
                bindingResult.addError(new FieldError("registerDto", "email", "Email already exists"));
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("registerDto", registerDto);
            model.addAttribute("step", 1);
            model.addAttribute("totalSteps", 4);
            return "register";
        }

        // Store in session
        MultiStepRegistrationDto registrationData = new MultiStepRegistrationDto();
        registrationData.setUserDetails(registerDto);
        session.setAttribute("registrationData", registrationData);

        return "redirect:/register/step2";
    }

    // Step 2: Child Information
    @GetMapping("/step2")
    public String showStep2(Model model, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        if (registrationData.getChildren() == null || registrationData.getChildren().isEmpty()) {
            registrationData.setChildren(new ArrayList<>());
            registrationData.getChildren().add(new ChildDto());
        }

        model.addAttribute("children", registrationData.getChildren());
        model.addAttribute("step", 2);
        model.addAttribute("totalSteps", 4);
        return "register-step2";
    }

    @PostMapping("/step2")
    public String handleStep2(@RequestParam(value = "childName", required = false) List<String> childNames,
                             @RequestParam(value = "childAge", required = false) List<Integer> childAges,
                             @RequestParam(value = "childDob", required = false) List<String> childDobs,
                             @RequestParam(value = "hearingLossType", required = false) List<String> hearingLossTypes,
                             @RequestParam(value = "equipmentType", required = false) List<String> equipmentTypes,
                             @RequestParam(value = "siblingsNames", required = false) List<String> siblingsNames,
                             @RequestParam(value = "chapterLocation", required = false) List<String> chapterLocations,
                             @RequestParam(value = "action", required = false) String action,
                             Model model,
                             HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        if ("addChild".equals(action)) {
            // Add a new empty child
            if (registrationData.getChildren() == null) {
                registrationData.setChildren(new ArrayList<>());
            }
            registrationData.getChildren().add(new ChildDto());
            session.setAttribute("registrationData", registrationData);
            return "redirect:/register/step2";
        }

        // Process child data
        List<ChildDto> children = new ArrayList<>();
        if (childNames != null && !childNames.isEmpty()) {
            for (int i = 0; i < childNames.size(); i++) {
                String name = childNames.get(i);
                if (name != null && !name.trim().isEmpty()) {
                    ChildDto child = new ChildDto();
                    child.setName(name);
                    if (childAges != null && i < childAges.size()) {
                        child.setAge(childAges.get(i));
                    }
                    if (childDobs != null && i < childDobs.size() && childDobs.get(i) != null && !childDobs.get(i).isEmpty()) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                            child.setDateOfBirth(sdf.parse(childDobs.get(i)));
                        } catch (Exception e) {
                            // Invalid date format
                        }
                    }
                    if (hearingLossTypes != null && i < hearingLossTypes.size()) {
                        child.setHearingLossType(hearingLossTypes.get(i));
                    }
                    if (equipmentTypes != null && i < equipmentTypes.size()) {
                        child.setEquipmentType(equipmentTypes.get(i));
                    }
                    if (siblingsNames != null && i < siblingsNames.size()) {
                        child.setSiblingsNames(siblingsNames.get(i));
                    }
                    if (chapterLocations != null && i < chapterLocations.size()) {
                        child.setChapterLocation(chapterLocations.get(i));
                    }
                    children.add(child);
                }
            }
        }

        registrationData.setChildren(children);
        session.setAttribute("registrationData", registrationData);

        return "redirect:/register/step3";
    }

    // Step 3: Membership Selection
    @GetMapping("/step3")
    public String showStep3(Model model, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        List<Membership> memberships = membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
        model.addAttribute("memberships", memberships);
        model.addAttribute("selectedMembershipId", registrationData.getSelectedMembershipId());
        model.addAttribute("step", 3);
        model.addAttribute("totalSteps", 4);
        return "register-step3";
    }

    @PostMapping("/step3")
    public String handleStep3(@RequestParam("membershipId") Integer membershipId,
                              HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        if (membershipId == null) {
            return "redirect:/register/step3";
        }

        registrationData.setSelectedMembershipId(membershipId);
        registrationData.setCartMembershipId(membershipId);
        session.setAttribute("registrationData", registrationData);

        return "redirect:/register/step4";
    }

    // Step 4: Cart View/Management
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

        Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getCartMembershipId());
        if (membershipOpt.isEmpty()) {
            return "redirect:/register/step3";
        }

        Membership membership = membershipOpt.get();
        model.addAttribute("membership", membership);
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
            System.out.println("Step4 POST - Action: " + action);
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
            if (registrationData == null) {
                System.out.println("Step4 POST - No registration data in session");
                return "redirect:/register";
            }

            if ("remove".equals(action)) {
                // Remove from cart - go back to membership selection
                registrationData.setCartMembershipId(null);
                registrationData.setSelectedMembershipId(null);
                session.setAttribute("registrationData", registrationData);
                return "redirect:/register/step3";
            }

            // Proceed to checkout or complete registration
            if (registrationData.getCartMembershipId() == null) {
                System.out.println("Step4 POST - No cart membership ID");
                return "redirect:/register/step3";
            }

            System.out.println("Step4 POST - Cart membership ID: " + registrationData.getCartMembershipId());
            Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getCartMembershipId());
            if (membershipOpt.isEmpty()) {
                System.out.println("Step4 POST - Membership not found");
                return "redirect:/register/step3";
            }

            Membership membership = membershipOpt.get();
            System.out.println("Step4 POST - Membership: " + membership.getName() + ", IsFree: " + membership.isFree());

            if (membership.isFree()) {
                // Free membership - complete registration directly
                System.out.println("Step4 POST - Completing registration for free membership");
                return completeRegistration(session);
            } else {
                // Paid membership - go to checkout
                System.out.println("Step4 POST - Redirecting to checkout");
                return "redirect:/register/checkout";
            }
        } catch (Exception e) {
            System.err.println("Error in step4: " + e.getMessage());
            e.printStackTrace();
            // Log the full stack trace
            e.printStackTrace();
            // Always redirect, never return a view
            return "redirect:/register/step4?error=processing_failed";
        }
    }

    // Checkout for paid memberships
    @GetMapping("/checkout")
    public String showCheckout(Model model, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            return "redirect:/register";
        }

        if (registrationData.getCartMembershipId() == null) {
            return "redirect:/register/step3";
        }

        Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getCartMembershipId());
        if (membershipOpt.isEmpty()) {
            return "redirect:/register/step3";
        }

        Membership membership = membershipOpt.get();
        if (membership.isFree()) {
            // Free membership shouldn't reach checkout
            return completeRegistration(session);
        }

        model.addAttribute("membership", membership);
        model.addAttribute("totalAmount", membership.getPrice());
        return "checkout";
    }

    @PostMapping("/checkout")
    public String handleCheckout(@RequestParam("cardNumber") String cardNumber,
                                @RequestParam("cardHolderName") String cardHolderName,
                                @RequestParam("expiryMonth") String expiryMonth,
                                @RequestParam("expiryYear") String expiryYear,
                                @RequestParam("cvv") String cvv,
                                Model model,
                                HttpSession session) {
        try {
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
            if (registrationData == null) {
                return "redirect:/register";
            }

            // Basic validation (in production, use proper payment gateway)
            if (cardNumber == null || cardNumber.trim().isEmpty() ||
                cardHolderName == null || cardHolderName.trim().isEmpty() ||
                expiryMonth == null || expiryMonth.trim().isEmpty() ||
                expiryYear == null || expiryYear.trim().isEmpty() ||
                cvv == null || cvv.trim().isEmpty()) {
                model.addAttribute("error", "All payment fields are required");
                Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getCartMembershipId());
                if (membershipOpt.isPresent()) {
                    model.addAttribute("membership", membershipOpt.get());
                    model.addAttribute("totalAmount", membershipOpt.get().getPrice());
                }
                return "checkout";
            }

            // In a real application, process payment here
            // For now, we'll just complete the registration

            return completeRegistration(session);
        } catch (Exception e) {
            System.err.println("Error in checkout: " + e.getMessage());
            e.printStackTrace();
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
            if (registrationData != null) {
                Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getCartMembershipId());
                if (membershipOpt.isPresent()) {
                    model.addAttribute("membership", membershipOpt.get());
                    model.addAttribute("totalAmount", membershipOpt.get().getPrice());
                }
            }
            model.addAttribute("error", "An error occurred processing your payment. Please try again.");
            return "checkout";
        }
    }

    // Complete registration and create account
    private String completeRegistration(HttpSession session) {
        try {
            System.out.println("Starting completeRegistration");
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
            if (registrationData == null || registrationData.getUserDetails() == null) {
                System.out.println("completeRegistration - No registration data or user details");
                return "redirect:/register";
            }

            RegisterDto userDetails = registrationData.getUserDetails();
            System.out.println("completeRegistration - Creating user: " + userDetails.getEmail());

            // Create user
            User user = User.builder()
                    .name(userDetails.getName())
                    .email(userDetails.getEmail())
                    .password(passwordEncoder.encode(userDetails.getPassword()))
                    .phone(userDetails.getPhone())
                    .address(userDetails.getAddress())
                    .postalCode(userDetails.getPostalCode())
                    .role(Role.USER.name())
                    .creation(new Date())
                    .build();

            // Set membership if selected
            if (registrationData.getSelectedMembershipId() != null) {
                Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getSelectedMembershipId());
                membershipOpt.ifPresent(user::setMembership);
            }

            user = userRepository.save(user);

            // Save children
            if (registrationData.getChildren() != null && !registrationData.getChildren().isEmpty()) {
                List<Child> children = new ArrayList<>();
                for (ChildDto childDto : registrationData.getChildren()) {
                    if (childDto.getName() != null && !childDto.getName().trim().isEmpty()) {
                        Child child = Child.builder()
                                .name(childDto.getName())
                                .age(childDto.getAge())
                                .dateOfBirth(childDto.getDateOfBirth())
                                .hearingLossType(childDto.getHearingLossType())
                                .equipmentType(childDto.getEquipmentType())
                                .siblingsNames(childDto.getSiblingsNames())
                                .chapterLocation(childDto.getChapterLocation())
                                .user(user)
                                .build();
                        children.add(child);
                    }
                }
                if (!children.isEmpty()) {
                    childRepository.saveAll(children);
                }
            }

            // Create cart if paid membership
            if (registrationData.getSelectedMembershipId() != null) {
                Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getSelectedMembershipId());
                if (membershipOpt.isPresent() && !membershipOpt.get().isFree()) {
                    Cart cart = Cart.builder()
                            .user(user)
                            .createdAt(new Date())
                            .updatedAt(new Date())
                            .build();
                    cart = cartRepository.save(cart);

                    Membership membership = membershipOpt.get();
                    CartItem cartItem = CartItem.builder()
                            .cart(cart)
                            .membership(membership)
                            .quantity(1)
                            .unitPrice(membership.getPrice())
                            .totalPrice(membership.getPrice())
                            .build();
                    cartItemRepository.save(cartItem);
                }
            }

            // Auto-login the user
            try {
                UserDetails userDetails1 = userService.loadUserByUsername(user.getEmail());
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails1, null, userDetails1.getAuthorities());
                
                // Set authentication in SecurityContext
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
                
                // Store in session using Spring Security's session key
                session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
                
                System.out.println("Auto-login successful for: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("Auto-login failed: " + e.getMessage());
                e.printStackTrace();
                // If auto-login fails, redirect to login page with message
                return "redirect:/login?registered=true";
            }

            // Clear session
            session.removeAttribute("registrationData");

            // Redirect to dashboard
            System.out.println("completeRegistration - Registration complete, redirecting to profile");
            return "redirect:/profile";
        } catch (Exception e) {
            System.err.println("Error completing registration: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/register?error=registration_failed";
        }
    }
}
