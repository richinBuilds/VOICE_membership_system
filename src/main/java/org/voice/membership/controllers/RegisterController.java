package org.voice.membership.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.voice.membership.dtos.RegisterDto;
import org.voice.membership.entities.User;
import org.voice.membership.entities.Role;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/register")
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String showRegister(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PostMapping
    public String handleRegister(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
                                 BindingResult bindingResult,
                                 Model model) {
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
            return "register";
        }

        // Create and persist user
        User user = User.builder()
                .name(registerDto.getName())
                .email(registerDto.getEmail())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .phone(registerDto.getPhone())
                .address(registerDto.getAddress())
                .role(Role.USER.name())
                .creation(new Date())
                .build();
        userRepository.save(user);

        // Redirect to login after successful registration
        return "redirect:/login";
    }
}
