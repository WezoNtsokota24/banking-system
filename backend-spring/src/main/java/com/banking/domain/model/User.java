package com.banking.domain.model;

/**
 * User domain entity for authentication.
 * Represents a user in the system with credentials.
 */
public class User {
    private Long id;
    private String username;
    private String passwordHash;

    public User(Long id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
