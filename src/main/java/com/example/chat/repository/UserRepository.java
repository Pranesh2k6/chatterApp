package com.example.chat.repository;

import com.example.chat.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based data access for the `users` table.
 * All queries use parameterised placeholders (?) to prevent SQL injection.
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Row mapper ─────────────────────────────────────────

    private static final RowMapper<User> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setOnline(rs.getBoolean("is_online"));
        Timestamp ls = rs.getTimestamp("last_seen");
        u.setLastSeen(ls != null ? ls.toLocalDateTime() : null);
        Timestamp ca = rs.getTimestamp("created_at");
        u.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        return u;
    };

    // ── Queries ────────────────────────────────────────────

    /** Insert a new user and return the generated id. */
    public void save(User user) {
        jdbc.update(
            "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
            user.getUsername(), user.getEmail(), user.getPasswordHash()
        );
    }

    public Optional<User> findByUsername(String username) {
        List<User> list = jdbc.query(
            "SELECT * FROM users WHERE username = ?", ROW_MAPPER, username
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findByEmail(String email) {
        List<User> list = jdbc.query(
            "SELECT * FROM users WHERE email = ?", ROW_MAPPER, email
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findById(Long id) {
        List<User> list = jdbc.query(
            "SELECT * FROM users WHERE id = ?", ROW_MAPPER, id
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Search users whose username contains the given term (case-insensitive).
     * Excludes the user performing the search.
     */
    public List<User> searchByUsername(String term, Long excludeUserId) {
        String pattern = "%" + term + "%";
        return jdbc.query(
            "SELECT * FROM users WHERE username LIKE ? AND id != ? ORDER BY username LIMIT 20",
            ROW_MAPPER, pattern, excludeUserId
        );
    }

    /** Get all currently-online users, excluding a given user. */
    public List<User> findOnlineUsers(Long excludeUserId) {
        return jdbc.query(
            "SELECT * FROM users WHERE is_online = TRUE AND id != ? ORDER BY username",
            ROW_MAPPER, excludeUserId
        );
    }

    // ── Presence ───────────────────────────────────────────

    /** Heartbeat: mark user online and update last_seen. */
    public void updatePresence(Long userId) {
        jdbc.update(
            "UPDATE users SET is_online = TRUE, last_seen = NOW() WHERE id = ?",
            userId
        );
    }

    /** Mark a specific user offline and set last_seen. */
    public void setOffline(Long userId) {
        jdbc.update(
            "UPDATE users SET is_online = FALSE, last_seen = NOW() WHERE id = ?",
            userId
        );
    }

    /** Mark all users offline (used on application startup). */
    public void setAllOffline() {
        jdbc.update("UPDATE users SET is_online = FALSE");
    }

    /**
     * Mark users offline if their last_seen is older than the given seconds.
     * Covers users who closed the browser without logging out.
     */
    public void expireStaleUsers(int staleSeconds) {
        jdbc.update(
            "UPDATE users SET is_online = FALSE WHERE is_online = TRUE AND last_seen < DATE_SUB(NOW(), INTERVAL ? SECOND)",
            staleSeconds
        );
    }

    /** Check if username already exists (for registration validation). */
    public boolean existsByUsername(String username) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username
        );
        return count != null && count > 0;
    }

    /** Check if email already exists (for registration validation). */
    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email
        );
        return count != null && count > 0;
    }
}
