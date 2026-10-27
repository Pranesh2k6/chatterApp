package com.example.chat.repository;

import com.example.chat.model.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

/**
 * JDBC-based data access for the `messages` table.
 */
@Repository
public class MessageRepository {

    private final JdbcTemplate jdbc;

    public MessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Maps a row to a Message, including the sender's username via JOIN alias. */
    private static final RowMapper<Message> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setConversationId(rs.getLong("conversation_id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setContent(rs.getString("content"));
        Timestamp sa = rs.getTimestamp("sent_at");
        m.setSentAt(sa != null ? sa.toLocalDateTime() : null);
        m.setRead(rs.getBoolean("is_read"));
        m.setNotified(rs.getBoolean("is_notified"));
        // Try to read the joined username column; it may not be present in every query
        try {
            m.setSenderUsername(rs.getString("sender_username"));
        } catch (Exception ignored) {
            // Column not present in this query – leave null
        }
        return m;
    };

    // ── Commands ───────────────────────────────────────────

    /** Insert a new message. */
    public void save(Message message) {
        jdbc.update(
            "INSERT INTO messages (conversation_id, sender_id, content) VALUES (?, ?, ?)",
            message.getConversationId(), message.getSenderId(), message.getContent()
        );
    }

    /**
     * Mark all messages in a conversation that were sent by otherUserId as read.
     * Called when the current user opens / views the conversation.
     */
    public void markAsRead(Long conversationId, Long otherUserId) {
        jdbc.update(
            "UPDATE messages SET is_read = TRUE WHERE conversation_id = ? AND sender_id = ? AND is_read = FALSE",
            conversationId, otherUserId
        );
    }

    /**
     * Mark messages as notified for a given user (the recipient).
     * Called after the sidebar renders the notification sound.
     */
    public void markAsNotified(Long userId) {
        jdbc.update(
            "UPDATE messages SET is_notified = TRUE WHERE sender_id != ? AND is_notified = FALSE "
            + "AND conversation_id IN ("
            + "  SELECT id FROM conversations WHERE user_one_id = ? OR user_two_id = ?"
            + ")",
            userId, userId, userId
        );
    }

    /**
     * Check whether there are un-notified messages for a user (used to trigger the sound).
     */
    public boolean hasUnnotifiedMessages(Long userId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM messages m "
            + "JOIN conversations c ON m.conversation_id = c.id "
            + "WHERE m.sender_id != ? AND m.is_notified = FALSE AND m.is_read = FALSE "
            + "AND (c.user_one_id = ? OR c.user_two_id = ?)",
            Integer.class, userId, userId, userId
        );
        return count != null && count > 0;
    }

    // ── Queries ────────────────────────────────────────────

    /**
     * Fetch the most recent `limit` messages for a conversation, oldest first.
     * JOINs with users to get the sender's username.
     */
    public List<Message> findByConversation(Long conversationId, int limit) {
        // Sub-select to get the newest N, then re-order oldest first
        String sql = """
            SELECT * FROM (
                SELECT m.id, m.conversation_id, m.sender_id, m.content,
                       m.sent_at, m.is_read, m.is_notified,
                       u.username AS sender_username
                FROM messages m
                JOIN users u ON u.id = m.sender_id
                WHERE m.conversation_id = ?
                ORDER BY m.sent_at DESC
                LIMIT ?
            ) sub
            ORDER BY sub.sent_at ASC
            """;
        return jdbc.query(sql, ROW_MAPPER, conversationId, limit);
    }

    /** Count total messages in a conversation (for "load older" logic). */
    public int countByConversation(Long conversationId) {
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM messages WHERE conversation_id = ?",
            Integer.class, conversationId
        );
        return c != null ? c : 0;
    }
}
