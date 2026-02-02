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
/**
 * Ensures a default admin user account exists on application startup.
 * Creates or updates the admin user with a known email and password.
 */
public class AdminUserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "tarparakrimy@gmail.com";
        User existingAdmin = userRepository.findByEmail(adminEmail);

        if (existingAdmin == null) {
            User admin = User.builder()
                    .firstName("Admin")
                    .middleName(null)
                    .lastName("User")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Caspstone36!"))
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

            existingAdmin.setPassword(passwordEncoder.encode("Caspstone36!"));
            existingAdmin.setRole(Role.ADMIN.name());
            existingAdmin.setFirstName("Admin");
            existingAdmin.setMiddleName(null);
            existingAdmin.setLastName("User");
            userRepository.save(existingAdmin);
            log.info("Admin credentials updated successfully. Email: {}", adminEmail);
        }
    }
}
