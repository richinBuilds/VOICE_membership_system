package org.voice.membership.services;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.voice.membership.dtos.MultiStepRegistrationDto;
import org.voice.membership.dtos.RegisterDto;

@Service
@RequiredArgsConstructor
public class RegistrationStep1Service {

    private final RegistrationService registrationService;

    public void validate(RegisterDto registerDto, BindingResult bindingResult) {
        if (registerDto.getEmail() != null && registrationService.isEmailTaken(registerDto.getEmail())) {
            bindingResult.addError(new FieldError("registerDto", "email", "Email already exists"));
        }
    }

    public void storeRegistration(HttpSession session, RegisterDto registerDto) {
        MultiStepRegistrationDto registrationData = new MultiStepRegistrationDto();
        registrationData.setUserDetails(registerDto);
        session.setAttribute("registrationData", registrationData);
    }
}
