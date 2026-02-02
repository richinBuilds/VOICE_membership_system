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
import org.voice.membership.services.EmailSenderService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/register")
/**
 * Implements the multi-step user registration, child details, and membership
 * checkout flow.
 * Guides users through all registration steps and persists users, children,
 * carts, and memberships.
 */
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

    @Autowired
    private EmailSenderService emailSenderService;

    @GetMapping
    public String showRegister(Model model, HttpSession session) {
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
        if (registerDto.getPassword() != null && registerDto.getConfirmPassword() != null
                && !registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            bindingResult.addError(new FieldError("registerDto", "confirmPassword", "Passwords do not match"));
        }

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

        MultiStepRegistrationDto registrationData = new MultiStepRegistrationDto();
        registrationData.setUserDetails(registerDto);
        session.setAttribute("registrationData", registrationData);

        return "redirect:/register/step2";
    }

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
            if (registrationData.getChildren() == null) {
                registrationData.setChildren(new ArrayList<>());
            }
            registrationData.getChildren().add(new ChildDto());
            session.setAttribute("registrationData", registrationData);
            return "redirect:/register/step2";
        }

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
                    if (childDobs != null && i < childDobs.size() && childDobs.get(i) != null
                            && !childDobs.get(i).isEmpty()) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                            child.setDateOfBirth(sdf.parse(childDobs.get(i)));
                        } catch (Exception e) {
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

    @GetMapping("/step3")
    public String showStep3(Model model, HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");

        if (registrationData == null) {
            return "redirect:/register";
        }

        List<Membership> memberships = membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
        model.addAttribute("memberships", memberships);
        model.addAttribute("selectedMembershipId", registrationData.getSelectedMembershipId());
        model.addAttribute("lineSeparator", System.lineSeparator());
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

        if (registrationData.getSelectedMembershipId() != null) {
            // Replacing previous membership selection
        }

        registrationData.setSelectedMembershipId(membershipId);
        registrationData.setCartMembershipId(membershipId);
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

        Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getCartMembershipId());
        if (membershipOpt.isEmpty()) {
            return "redirect:/register/step3";
        }

        Membership membership = membershipOpt.get();
        model.addAttribute("membership", membership);
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
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session
                    .getAttribute("registrationData");
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

            Optional<Membership> membershipOpt = membershipRepository.findById(registrationData.getCartMembershipId());
            if (membershipOpt.isEmpty()) {
                return "redirect:/register/step3";
            }

            Membership membership = membershipOpt.get();

            if (membership.isFree()) {
                return completeRegistration(session);
            } else {
                return "redirect:/register/checkout";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/register/step4?error=processing_failed";
        }
    }

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
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session
                    .getAttribute("registrationData");
            if (registrationData == null) {
                return "redirect:/register";
            }

            if (cardNumber == null || cardNumber.trim().isEmpty() ||
                    cardHolderName == null || cardHolderName.trim().isEmpty() ||
                    expiryMonth == null || expiryMonth.trim().isEmpty() ||
                    expiryYear == null || expiryYear.trim().isEmpty() ||
                    cvv == null || cvv.trim().isEmpty()) {
                model.addAttribute("error", "All payment fields are required");
                Optional<Membership> membershipOpt = membershipRepository
                        .findById(registrationData.getCartMembershipId());
                if (membershipOpt.isPresent()) {
                    model.addAttribute("membership", membershipOpt.get());
                    model.addAttribute("totalAmount", membershipOpt.get().getPrice());
                }
                return "checkout";
            }
            return completeRegistration(session);
        } catch (Exception e) {
            e.printStackTrace();
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session
                    .getAttribute("registrationData");
            if (registrationData != null) {
                Optional<Membership> membershipOpt = membershipRepository
                        .findById(registrationData.getCartMembershipId());
                if (membershipOpt.isPresent()) {
                    model.addAttribute("membership", membershipOpt.get());
                    model.addAttribute("totalAmount", membershipOpt.get().getPrice());
                }
            }
            model.addAttribute("error", "An error occurred processing your payment. Please try again.");
            return "checkout";
        }
    }

    private String completeRegistration(HttpSession session) {
        try {
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session
                    .getAttribute("registrationData");
            if (registrationData == null || registrationData.getUserDetails() == null) {
                return "redirect:/register";
            }

            RegisterDto userDetails = registrationData.getUserDetails();

            User user = User.builder()
                    .firstName(userDetails.getFirstName())
                    .middleName(userDetails.getMiddleName())
                    .lastName(userDetails.getLastName())
                    .email(userDetails.getEmail())
                    .password(passwordEncoder.encode(userDetails.getPassword()))
                    .phone(userDetails.getPhone())
                    .address(userDetails.getAddress())
                    .city(userDetails.getCity())
                    .province(userDetails.getProvince())
                    .postalCode(userDetails.getPostalCode())
                    .role(Role.USER.name())
                    .creation(new Date())
                    .build();

            if (registrationData.getSelectedMembershipId() != null) {
                Optional<Membership> membershipOpt = membershipRepository
                        .findById(registrationData.getSelectedMembershipId());
                if (membershipOpt.isPresent()) {
                    Membership membership = membershipOpt.get();
                    user.setMembership(membership);

                    if (!membership.isFree()) {
                        Date now = new Date();
                        user.setMembershipStartDate(now);

                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(now);
                        cal.add(java.util.Calendar.YEAR, 1);
                        user.setMembershipExpiryDate(cal.getTime());
                    }
                }
            }

            user = userRepository.save(user);

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

            if (registrationData.getSelectedMembershipId() != null) {
                Optional<Membership> membershipOpt = membershipRepository
                        .findById(registrationData.getSelectedMembershipId());
                if (membershipOpt.isPresent() && !membershipOpt.get().isFree()) {
                    Optional<Cart> existingCartOpt = cartRepository.findByUserId(user.getId());

                    Cart cart;
                    if (existingCartOpt.isPresent()) {
                        Cart existingCart = existingCartOpt.get();
                        cartItemRepository.deleteByCartId(existingCart.getId());
                        cart = existingCart;
                        cart.setUpdatedAt(new Date());
                        cart = cartRepository.save(cart);
                    } else {
                        cart = Cart.builder()
                                .user(user)
                                .createdAt(new Date())
                                .updatedAt(new Date())
                                .build();
                        cart = cartRepository.save(cart);
                    }

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

            try {
                UserDetails userDetails1 = userService.loadUserByUsername(user.getEmail());
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails1, null, userDetails1.getAuthorities());

                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);

                session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
            } catch (Exception e) {

                return "redirect:/login?registered=true";
            }

            session.removeAttribute("registrationData");

            return "redirect:/profile";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/register?error=registration_failed";
        }
    }

    @GetMapping("/upgrade-checkout")
    public String showUpgradeCheckout(Model model, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            Membership currentMembership = user.getMembership();
            if (currentMembership == null || !currentMembership.isFree()) {
                return "redirect:/profile?error=not_eligible_for_upgrade";
            }

            model.addAttribute("user", user);
            String fullName = user.getFirstName() +
                    (user.getMiddleName() != null && !user.getMiddleName().isEmpty() ? " " + user.getMiddleName() : "")
                    +
                    " " + user.getLastName();
            model.addAttribute("userName", fullName);
            model.addAttribute("userEmail", user.getEmail());

            return "upgrade-checkout";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile?error=checkout_load_failed";
        }
    }

    @PostMapping("/upgrade-checkout")
    public String handleUpgradeCheckout(@RequestParam("membershipId") Integer membershipId,
            @RequestParam("cardNumber") String cardNumber,
            @RequestParam("cardHolderName") String cardHolderName,
            @RequestParam("expiryMonth") String expiryMonth,
            @RequestParam("expiryYear") String expiryYear,
            @RequestParam("cvv") String cvv,
            Model model,
            Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            Membership currentMembership = user.getMembership();
            if (currentMembership == null || !currentMembership.isFree()) {
                return "redirect:/profile?error=not_eligible_for_upgrade";
            }

            Optional<Membership> paidMembershipOpt = membershipRepository.findById(membershipId);
            if (paidMembershipOpt.isEmpty() || paidMembershipOpt.get().isFree()) {
                return "redirect:/profile?error=invalid_membership";
            }

            if (cardNumber == null || cardNumber.trim().isEmpty() ||
                    cardHolderName == null || cardHolderName.trim().isEmpty() ||
                    expiryMonth == null || expiryMonth.trim().isEmpty() ||
                    expiryYear == null || expiryYear.trim().isEmpty() ||
                    cvv == null || cvv.trim().isEmpty()) {
                model.addAttribute("error", "All payment fields are required");
                model.addAttribute("user", user);
                String fullName = user.getFirstName() +
                        (user.getMiddleName() != null && !user.getMiddleName().isEmpty() ? " " + user.getMiddleName()
                                : "")
                        +
                        " " + user.getLastName();
                model.addAttribute("userName", fullName);
                model.addAttribute("upgradeMembership", paidMembershipOpt.get());
                return "upgrade-checkout";
            }

            Membership paidMembership = paidMembershipOpt.get();

            user.setMembership(paidMembership);

            Date now = new Date();
            user.setMembershipStartDate(now);

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(now);
            cal.add(java.util.Calendar.YEAR, 1);
            Date expiryDate = cal.getTime();
            user.setMembershipExpiryDate(expiryDate);

            userRepository.save(user);

            System.out.println("=== MEMBERSHIP UPDATED ===");
            System.out.println("Status: Paid/Active");
            System.out.println("Start Date: " + now);
            System.out.println("Expiry Date: " + expiryDate);

            try {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd, yyyy");
                String fullName = user.getFirstName() +
                        (user.getMiddleName() != null && !user.getMiddleName().isEmpty() ? " " + user.getMiddleName()
                                : "")
                        +
                        " " + user.getLastName();
                emailSenderService.sendMembershipUpgradeConfirmation(
                        user.getEmail(),
                        fullName,
                        paidMembership.getName(),
                        dateFormat.format(expiryDate));
                System.out.println("Confirmation email sent to: " + user.getEmail());
            } catch (Exception emailEx) {
                System.err.println("Warning: Failed to send confirmation email: " + emailEx.getMessage());
            }

            return "redirect:/profile?upgrade=success";
        } catch (Exception e) {
            System.err.println("=== UPGRADE PAYMENT ERROR ===");
            System.err.println("Error in upgrade checkout: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("error",
                    "An error occurred processing your payment. Please try again or contact support if the issue persists.");

            try {
                User user = userRepository.findByEmail(principal.getName());
                if (user != null) {
                    model.addAttribute("user", user);
                    String fullName = user.getFirstName() +
                            (user.getMiddleName() != null && !user.getMiddleName().isEmpty()
                                    ? " " + user.getMiddleName()
                                    : "")
                            +
                            " " + user.getLastName();
                    model.addAttribute("userName", fullName);

                    Optional<Membership> paidMembershipOpt = membershipRepository.findById(membershipId);
                    paidMembershipOpt.ifPresent(membership -> model.addAttribute("upgradeMembership", membership));
                }
            } catch (Exception ex2) {
                System.err.println("Error loading user for error page: " + ex2.getMessage());
            }

            return "upgrade-checkout";
        }
    }
}
