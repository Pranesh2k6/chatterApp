package com.example.chat.service;

import com.example.chat.model.User;
import com.example.chat.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for user registration, lookup, and search.
 */
@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user.
     * @return empty string on success, or an error message.
     */
    public String register(String username, String email, String password) {
        // Normalise email
        email = email.trim().toLowerCase();

        // Pre-check for duplicates (friendly messages)
        if (userRepo.existsByUsername(username)) {
            return "That username is already taken.";
        }
        if (userRepo.existsByEmail(email)) {
            return "An account with that email already exists.";
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        try {
            userRepo.save(user);
        } catch (DuplicateKeyException ex) {
            // Race condition: another request inserted between our check and insert
            String msg = ex.getMessage();
            if (msg != null && msg.contains("username")) {
                return "That username is already taken.";
            }
            return "An account with that email already exists.";
        }
        return "";
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepo.findById(id);
    }

    public List<User> search(String term, Long excludeUserId) {
        return userRepo.searchByUsername(term, excludeUserId);
    }

    public List<User> getOnlineUsers(Long excludeUserId) {
        return userRepo.findOnlineUsers(excludeUserId);
    }
}
