package com.banking.domain.port;

/**
 * Port for JWT token operations.
 * Keeps the domain layer pure by abstracting JWT generation and validation.
 */
public interface JwtPort {

    /**
     * Generates a JWT token for the given username.
     *
     * @param username the username to include in the token
     * @return the generated JWT token as a string
     */
    String generateToken(String username);

    /**
     * Validates a JWT token and extracts the username.
     *
     * @param token the JWT token to validate
     * @return the username if valid, null otherwise
     */
    String validateToken(String token);
}
