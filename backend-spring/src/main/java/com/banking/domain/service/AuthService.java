package com.banking.domain.service;

import com.banking.domain.exception.AuthenticationException;
import com.banking.domain.model.User;
import com.banking.domain.port.JwtPort;
import com.banking.domain.port.PasswordEncoderPort;
import com.banking.domain.port.UserRepository;

/**
 * AuthService: Domain service for authentication.
 * Handles user authentication and JWT token generation.
 */
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtPort jwtPort;

    public AuthService(UserRepository userRepository, PasswordEncoderPort passwordEncoder, JwtPort jwtPort) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtPort = jwtPort;
    }

    /**
     * Authenticates a user with username and password.
     *
     * @param username the username
     * @param password the raw password
     * @return JWT token if authentication successful
     * @throws AuthenticationException if authentication fails
     */
    public String authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }

        return jwtPort.generateToken(username);
    }
}
