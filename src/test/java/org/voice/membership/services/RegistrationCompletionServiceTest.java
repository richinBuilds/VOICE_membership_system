package org.voice.membership.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.voice.membership.dtos.MultiStepRegistrationDto;
import org.voice.membership.dtos.RegisterDto;
import org.voice.membership.entities.User;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationCompletionServiceTest {

    @Mock
    private RegistrationService registrationService;

    @Test
    void completeRegistration_WithGoogleSignupUserId_ShouldPassGoogleUserIdToRegistrationService() {
        RegistrationCompletionService completionService = new RegistrationCompletionService(registrationService);

        MockHttpSession session = new MockHttpSession();
        RegisterDto registerDto = new RegisterDto();
        registerDto.setFirstName("Google");
        registerDto.setLastName("User");
        registerDto.setEmail("google.user@example.com");

        MultiStepRegistrationDto registrationData = new MultiStepRegistrationDto();
        registrationData.setUserDetails(registerDto);
        registrationData.setSelectedMembershipId(1);
        registrationData.setChildren(new ArrayList<>());

        session.setAttribute("registrationData", registrationData);
        session.setAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_USER_ID_SESSION_KEY, 42);

        when(registrationService.registerUser(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new User());

        String result = completionService.completeRegistration(session);

        assertThat(result).isEqualTo("redirect:/register/verification-sent");
        verify(registrationService).registerUser(
                eq(registerDto),
                eq(42),
                eq(1),
                eq(registrationData.getChildren()),
                isNull(),
                isNull(),
                isNull());
        assertThat(session.getAttribute("registrationData")).isNull();
        assertThat(session.getAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_USER_ID_SESSION_KEY)).isNull();
    }

    @Test
    void completeRegistration_WithoutRegistrationData_ShouldRedirectToRegister() {
        RegistrationCompletionService completionService = new RegistrationCompletionService(registrationService);

        MockHttpSession session = new MockHttpSession();

        String result = completionService.completeRegistration(session);

        assertThat(result).isEqualTo("redirect:/register");
    }
}
