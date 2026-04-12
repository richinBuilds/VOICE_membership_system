package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.dtos.ChildDto;
import org.voice.membership.dtos.RegisterDto;
import org.voice.membership.entities.Cart;
import org.voice.membership.entities.CartItem;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipPaymentTransaction;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.entities.VerificationToken;
import org.voice.membership.repositories.CartItemRepository;
import org.voice.membership.repositories.CartRepository;
import org.voice.membership.repositories.MembershipPaymentTransactionRepository;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.VerificationTokenRepository;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all business logic for the multi-step user registration workflow,
 * including user creation, child records, cart setup, payment recording,
 * email verification token generation, and verification resend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MembershipPaymentTransactionRepository paymentTransactionRepository;
    private final PayPalProperties payPalProperties;
    private final EmailSenderService emailSenderService;
    private final MembershipService membershipService;
    private final ChildService childService;
    private final AdminNotificationService adminNotificationService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    // -----------------------------------------------------------------------
    // Public result enums
    // -----------------------------------------------------------------------

    public enum VerificationOutcome {
        SUCCESS, INVALID_TOKEN, EXPIRED
    }

    public enum ResendOutcome {
        SUCCESS, NOT_FOUND, ALREADY_VERIFIED, EMAIL_SEND_FAILED
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the given email is already registered.
     */
    public boolean isEmailTaken(String email) {
        return !userRepository.findAllByEmailIgnoreCase(email).isEmpty();
    }

    /**
     * Completes the registration: creates (or updates) the user, saves children,
     * records any PayPal payment, creates a verification token, sends the
     * verification email, and sets up the membership cart entry.
     *
     * @param userDetails          basic user data from step 1
     * @param googleSignupUserId   ID of a Google-pre-created user stub, or
     *                             {@code null} for a standard sign-up
     * @param selectedMembershipId chosen membership ID
     * @param children             list of child DTOs (may be empty or null)
     * @param paypalOrderId        PayPal order ID if payment was captured,
     *                             otherwise {@code null}
     * @param paypalCaptureId      PayPal capture ID if payment was captured,
     *                             otherwise {@code null}
     * @param paymentAmount        captured payment amount, or {@code null}
     * @return the newly created (or updated) User
     * @throws NoSuchElementException if {@code googleSignupUserId} is non-null
     *                                but no matching user exists in the database
     */
    @Transactional
    public User registerUser(RegisterDto userDetails,
            Integer googleSignupUserId,
            Integer selectedMembershipId,
            List<ChildDto> children,
            String paypalOrderId,
            String paypalCaptureId,
            BigDecimal paymentAmount) {

        if (googleSignupUserId != null) {
            log.info("Google signup flow detected for userId: {}", googleSignupUserId);
        } else {
            log.info("Normal registration flow (new user)");
        }

        User user = buildOrUpdateUser(userDetails, googleSignupUserId);

        applySelectedMembership(user, selectedMembershipId);

        user = userRepository.save(user);

        notifyAdminOfNewMember(user);

        recordPaymentTransaction(user, paypalOrderId, paypalCaptureId, paymentAmount);

        issueAndSendVerificationToken(user);

        saveChildren(user, children);

        createMembershipCart(user, selectedMembershipId);

        return user;
    }

    /**
     * Verifies a user's email address using the provided token.
     *
     * @param token the verification token string
     * @return the outcome of the verification attempt
     */
    @Transactional
    public VerificationOutcome verifyEmail(String token) {
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return VerificationOutcome.INVALID_TOKEN;
        }
        VerificationToken verificationToken = tokenOpt.get();
        if (verificationToken.isExpired()) {
            return VerificationOutcome.EXPIRED;
        }
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);
        return VerificationOutcome.SUCCESS;
    }

    /**
     * Resends the verification email for the given email address.
     *
     * @param email the account email address
     * @return the outcome of the resend attempt
     */
    @Transactional
    public ResendOutcome resendVerification(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResendOutcome.NOT_FOUND;
        }
        if (user.isEmailVerified()) {
            return ResendOutcome.ALREADY_VERIFIED;
        }
        verificationTokenRepository.findByUser(user).ifPresent(existingToken -> {
            log.info("Deleting existing token for user ID: {}", user.getId());
            verificationTokenRepository.delete(existingToken);
            verificationTokenRepository.flush();
        });
        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(new VerificationToken(token, user));
        String verificationLink = appBaseUrl + "/register/verify?token=" + token;
        String userName = user.getFirstName() + " " + user.getLastName();
        try {
            emailSenderService.sendVerificationEmail(user.getEmail(), userName, verificationLink);
            return ResendOutcome.SUCCESS;
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            return ResendOutcome.EMAIL_SEND_FAILED;
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private User buildOrUpdateUser(RegisterDto userDetails, Integer googleSignupUserId) {
        if (googleSignupUserId != null) {
            User existing = userRepository.findById(googleSignupUserId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Google signup user not found: " + googleSignupUserId));
            existing.setFirstName(userDetails.getFirstName());
            existing.setMiddleName(userDetails.getMiddleName());
            existing.setLastName(userDetails.getLastName());
            existing.setEmail(userDetails.getEmail());
            existing.setPhone(userDetails.getPhone());
            existing.setAddress(userDetails.getAddress());
            existing.setCity(userDetails.getCity());
            existing.setProvince(userDetails.getProvince());
            existing.setPostalCode(userDetails.getPostalCode());
            if (existing.getRole() == null || existing.getRole().isBlank()) {
                existing.setRole(Role.USER.name());
            }
            if (existing.getCreation() == null) {
                existing.setCreation(new Date());
            }
            existing.setEmailVerified(false);
            return existing;
        }

        return User.builder()
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
                .emailVerified(false)
                .build();
    }

    private void applySelectedMembership(User user, Integer selectedMembershipId) {
        if (selectedMembershipId == null) {
            return;
        }
        Membership membership = membershipService.getMembershipById(selectedMembershipId).orElse(null);
        if (membership == null) {
            return;
        }
        user.setMembership(membership);
        if (!membership.isFree()) {
            Date now = new Date();
            user.setMembershipStartDate(now);
            user.setPaid(true);
            user.setMembershipExpiryDate(membershipService.calculateMembershipExpiry(now));
        } else {
            user.setPaid(false);
        }
    }

    private void notifyAdminOfNewMember(User user) {
        try {
            adminNotificationService.createInstantNotification(user);
        } catch (Exception e) {
            log.error("Failed to create admin notification for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private void recordPaymentTransaction(User user, String paypalOrderId,
            String paypalCaptureId, BigDecimal paymentAmount) {
        if (paypalOrderId == null || paypalCaptureId == null || paymentAmount == null) {
            return;
        }
        MembershipPaymentTransaction transaction = new MembershipPaymentTransaction();
        transaction.setUser(user);
        transaction.setMembership(user.getMembership());
        transaction.setPaypalOrderId(paypalOrderId);
        transaction.setPaypalCaptureId(paypalCaptureId);
        transaction.setAmount(paymentAmount);
        transaction.setCurrency(payPalProperties.getCurrency());
        transaction.setStatus("COMPLETED");
        transaction.setFailureReason(null);
        paymentTransactionRepository.save(transaction);
    }

    private void issueAndSendVerificationToken(User user) {
        log.info("Generating token for user ID: {}", user.getId());

        verificationTokenRepository.findByUser(user).ifPresent(verificationTokenRepository::delete);
        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(new VerificationToken(token, user));
        String verificationLink = appBaseUrl + "/register/verify?token=" + token;
        String userName = user.getFirstName() + " " + user.getLastName();
        try {
            emailSenderService.sendVerificationEmail(user.getEmail(), userName, verificationLink);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void saveChildren(User user, List<ChildDto> children) {
        if (children == null || children.isEmpty()) {
            return;
        }
        for (ChildDto childDto : children) {
            if (childDto.getName() == null || childDto.getName().trim().isEmpty()) {
                continue;
            }
            String dobStr = null;
            if (childDto.getDateOfBirth() != null) {
                dobStr = new SimpleDateFormat("yyyy-MM-dd").format(childDto.getDateOfBirth());
            }
            childService.createChild(user, childDto.getName(), childDto.getAge(), dobStr,
                    childDto.getHearingLossType(), childDto.getEquipmentType(),
                    childDto.getSiblingsNames(), childDto.getChapterLocation());
        }
    }

    private void createMembershipCart(User user, Integer selectedMembershipId) {
        if (selectedMembershipId == null) {
            return;
        }
        Membership membership = membershipService.getMembershipById(selectedMembershipId).orElse(null);
        if (membership == null || membership.isFree()) {
            return;
        }

        Cart cart;
        Optional<Cart> existingCartOpt = cartRepository.findByUserId(user.getId());
        if (existingCartOpt.isPresent()) {
            Cart existingCart = existingCartOpt.get();
            cartItemRepository.deleteByCartId(existingCart.getId());
            existingCart.setUpdatedAt(new Date());
            cart = cartRepository.save(existingCart);
        } else {
            cart = cartRepository.save(Cart.builder()
                    .user(user)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build());
        }

        cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(membership.getPrice())
                .totalPrice(membership.getPrice())
                .build());
    }
}
