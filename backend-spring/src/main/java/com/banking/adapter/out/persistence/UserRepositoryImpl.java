package com.banking.adapter.out.persistence;

import com.banking.domain.model.User;
import com.banking.domain.port.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter implementing UserRepository using JPA.
 */
@Component
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    public UserRepositoryImpl(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username)
                .map(entity -> new User(entity.getId(), entity.getUsername(), entity.getPasswordHash()));
    }
}
