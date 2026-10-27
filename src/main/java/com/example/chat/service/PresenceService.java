package com.example.chat.service;

import com.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Manages online/offline presence.
 *
 * - On startup: marks every user offline.
 * - Heartbeat: called by PresenceInterceptor on every authenticated request
 *   (throttled to one DB write per user every 10 s).
 * - Scheduled job: every 10 s, marks users offline if last_seen > 30 s ago.
 */
@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    /** Minimum interval between heartbeat DB writes for the same user (millis). */
    private static final long HEARTBEAT_INTERVAL_MS = 10_000;

    /** Users with last_seen older than this are set offline (seconds). */
    private static final int STALE_THRESHOLD_SECONDS = 30;

    private final UserRepository userRepo;

    /*
     * Simple in-memory throttle: maps userId → last heartbeat timestamp.
     * This is a ConcurrentHashMap so the interceptor is thread-safe.
     */
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> lastHeartbeat =
            new java.util.concurrent.ConcurrentHashMap<>();

    public PresenceService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // ── Startup: mark everyone offline ─────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        userRepo.setAllOffline();
        log.info("All users set offline on startup");
    }

    // ── Heartbeat (called by interceptor) ──────────────────

    /**
     * Update a user's presence, but skip the DB write if the last one
     * was less than HEARTBEAT_INTERVAL_MS ago.
     */
    public void heartbeat(Long userId) {
        long now = System.currentTimeMillis();
        Long last = lastHeartbeat.get(userId);
        if (last != null && (now - last) < HEARTBEAT_INTERVAL_MS) {
            return; // throttled
        }
        lastHeartbeat.put(userId, now);
        userRepo.updatePresence(userId);
    }

    // ── Login / logout ─────────────────────────────────────

    public void setOnline(Long userId) {
        userRepo.updatePresence(userId);
        lastHeartbeat.put(userId, System.currentTimeMillis());
    }

    public void setOffline(Long userId) {
        userRepo.setOffline(userId);
        lastHeartbeat.remove(userId);
    }

    // ── Scheduled cleanup ──────────────────────────────────

    /** Runs every 10 seconds. Sets stale users offline. */
    @Scheduled(fixedRate = 10_000)
    public void expireStaleUsers() {
        userRepo.expireStaleUsers(STALE_THRESHOLD_SECONDS);
    }
}
