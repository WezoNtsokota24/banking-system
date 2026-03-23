package com.banking.domain.port;

/**
 * Port for password encoding and verification operations.
 * Keeps the domain layer pure by abstracting password hashing.
 */
public interface PasswordEncoderPort {

    /**
     * Encodes a raw password.
     *
     * @param rawPassword the raw password
     * @return the encoded password
     */
    String encode(String rawPassword);

    /**
     * Verifies if a raw password matches an encoded password.
     *
     * @param rawPassword the raw password
     * @param encodedPassword the encoded password
     * @return true if matches, false otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}
