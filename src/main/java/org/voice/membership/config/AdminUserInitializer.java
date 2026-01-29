package org.voice.membership.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;

@Slf4j
@Component
public class AdminUserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user already exists
        String adminEmail = "tarparakrimy@gmail.com";
        User existingAdmin = userRepository.findByEmail(adminEmail);

        if (existingAdmin == null) {
            // Create admin user
            User admin = User.builder()
                    .name("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Dasnadas36!"))
                    .phone("N/A")
                    .address("N/A")
                    .postalCode("N/A")
                    .role(Role.ADMIN.name())
                    .creation(new Date())
                    .build();

            userRepository.save(admin);
            log.info("Admin user created successfully. Email: {}", adminEmail);
        } else {
            log.info("Admin user already exists");

            // Update password and role to ensure correct credentials
            existingAdmin.setPassword(passwordEncoder.encode("Dasnadas36!"));
            existingAdmin.setRole(Role.ADMIN.name());
            existingAdmin.setName("Admin");
            userRepository.save(existingAdmin);
            log.info("Admin credentials updated successfully. Email: {}", adminEmail);
        }
    }
}
