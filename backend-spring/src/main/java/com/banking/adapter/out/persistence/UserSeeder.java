package com.banking.adapter.out.persistence;

import com.banking.domain.port.PasswordEncoderPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * UserSeeder: Seeds initial test user data on application startup.
 * Implements CommandLineRunner to execute seeding logic after application context is loaded.
 */
@Component
public class UserSeeder implements CommandLineRunner {

    private final SpringDataUserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public UserSeeder(SpringDataUserRepository userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if testuser already exists
        if (userRepository.findByUsername("testuser").isPresent()) {
            System.out.println("Test user 'testuser' already exists. Skipping seeding.");
            return;
        }

        // Create new test user
        String encodedPassword = passwordEncoder.encode("password");
        UserEntity testUser = new UserEntity("testuser", encodedPassword);

        // Save to database
        userRepository.save(testUser);

        System.out.println("Test user 'testuser' created successfully with password 'password'.");
    }
}
