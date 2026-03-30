package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.dtos.MultiStepRegistrationDto;
import org.voice.membership.dtos.RegisterDto;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    public static final String GOOGLE_AUTH_FLOW_SESSION_KEY = "GOOGLE_AUTH_FLOW";
    public static final String GOOGLE_AUTH_FLOW_SIGNUP = "signup";
    public static final String GOOGLE_SIGNUP_REDIRECT_STEP2_SESSION_KEY = "GOOGLE_SIGNUP_REDIRECT_STEP2";
    public static final String GOOGLE_SIGNUP_USER_ID_SESSION_KEY = "GOOGLE_SIGNUP_USER_ID";
    public static final String REGISTRATION_DATA_SESSION_KEY = "registrationData";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        boolean isSignupFlow = isGoogleSignupFlow();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equalsIgnoreCase(registrationId)) {
            return oauth2User;
        }

        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_account"),
                    "Google account did not return an email address.");
        }

        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null) {
            if (!isSignupFlow) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("google_signup_required"),
                        "No account found for this Google email. Please sign up first.");
            }
            user = createGoogleUser(oauth2User, email);
        } else {
            user = updateExistingGoogleUser(user, oauth2User);
        }

        if (!isSignupFlow && !user.isEmailVerified()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_unverified"),
                    "Please verify your email before logging in.");
        }

        if (isSignupFlow) {
            prepareRegistrationSessionForStep2(user);
        }

        if (user.isAccountLocked()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_locked"),
                    "Your account is locked. Please contact support or try again later.");
        }

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("email", user.getEmail());

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + normalizeRole(user.getRole())));

        return new DefaultOAuth2User(authorities, attributes, "email");
    }

    private User createGoogleUser(OAuth2User oauth2User, String email) {
        String firstName = firstNonBlank(
                oauth2User.getAttribute("given_name"),
                oauth2User.getAttribute("name"),
                "Google");

        String lastName = firstNonBlank(
                oauth2User.getAttribute("family_name"),
                "User");

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.USER.name())
                .creation(new Date())
            .emailVerified(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .paid(false)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingGoogleUser(User user, OAuth2User oauth2User) {
        String givenName = oauth2User.getAttribute("given_name");
        String familyName = oauth2User.getAttribute("family_name");

        if ((user.getFirstName() == null || user.getFirstName().isBlank()) && givenName != null && !givenName.isBlank()) {
            user.setFirstName(givenName);
        }

        if ((user.getLastName() == null || user.getLastName().isBlank()) && familyName != null && !familyName.isBlank()) {
            user.setLastName(familyName);
        }

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole(Role.USER.name());
        }

        return userRepository.save(user);
    }

    private String normalizeRole(String role) {
        return (role == null || role.isBlank()) ? Role.USER.name() : role;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean isGoogleSignupFlow() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }

        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object flow = session.getAttribute(GOOGLE_AUTH_FLOW_SESSION_KEY);
        session.removeAttribute(GOOGLE_AUTH_FLOW_SESSION_KEY);
        return GOOGLE_AUTH_FLOW_SIGNUP.equalsIgnoreCase(String.valueOf(flow));
    }

    private void prepareRegistrationSessionForStep2(User user) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpSession session = attributes.getRequest().getSession(true);

        RegisterDto registerDto = new RegisterDto();
        registerDto.setFirstName(user.getFirstName());
        registerDto.setMiddleName(user.getMiddleName());
        registerDto.setLastName(user.getLastName());
        registerDto.setEmail(user.getEmail());
        registerDto.setPhone(user.getPhone());
        registerDto.setAddress(user.getAddress());
        registerDto.setCity(user.getCity());
        registerDto.setProvince(user.getProvince());
        registerDto.setPostalCode(user.getPostalCode());

        MultiStepRegistrationDto registrationData = new MultiStepRegistrationDto();
        registrationData.setUserDetails(registerDto);

        session.setAttribute(REGISTRATION_DATA_SESSION_KEY, registrationData);
        session.setAttribute(GOOGLE_SIGNUP_REDIRECT_STEP2_SESSION_KEY, true);
        session.setAttribute(GOOGLE_SIGNUP_USER_ID_SESSION_KEY, user.getId());
    }
}