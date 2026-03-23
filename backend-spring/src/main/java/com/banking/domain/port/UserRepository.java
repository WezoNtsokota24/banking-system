package com.banking.domain.port;

import com.banking.domain.model.User;
import java.util.Optional;

/**
 * Port for user repository operations.
 */
public interface UserRepository {

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
}
