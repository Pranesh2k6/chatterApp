package com.example.chat.repository;

import com.example.chat.model.Conversation;
import com.example.chat.model.ConversationSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based data access for the `conversations` table.
 */
@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbc;

    public ConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Conversation> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Conversation c = new Conversation();
        c.setId(rs.getLong("id"));
        c.setUserOneId(rs.getLong("user_one_id"));
        c.setUserTwoId(rs.getLong("user_two_id"));
        Timestamp ca = rs.getTimestamp("created_at");
        c.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        return c;
    };

    // ── Queries ────────────────────────────────────────────

    public Optional<Conversation> findById(Long id) {
        List<Conversation> list = jdbc.query(
            "SELECT * FROM conversations WHERE id = ?", ROW_MAPPER, id
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Find a conversation between two users.
     * user_one_id is always the smaller id.
     */
    public Optional<Conversation> findByUsers(Long userA, Long userB) {
        Long small = Math.min(userA, userB);
        Long large = Math.max(userA, userB);
        List<Conversation> list = jdbc.query(
            "SELECT * FROM conversations WHERE user_one_id = ? AND user_two_id = ?",
            ROW_MAPPER, small, large
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Create a conversation between two users.
     * Returns the newly created conversation.
     */
    public void create(Long userA, Long userB) {
        Long small = Math.min(userA, userB);
        Long large = Math.max(userA, userB);
        jdbc.update(
            "INSERT INTO conversations (user_one_id, user_two_id) VALUES (?, ?)",
            small, large
        );
    }

    /**
     * Build the sidebar list for a user.
     * Each row contains:
     *   - conversation id, the other user's info
     *   - the latest message content and time
     *   - count of unread messages sent by the other user
     * Ordered by latest message time, newest first.
     */
    public List<ConversationSummary> findSummariesForUser(Long userId) {
        String sql = """
            SELECT
                c.id                            AS conv_id,
                other_user.id                   AS other_id,
                other_user.username             AS other_username,
                other_user.is_online            AS other_online,
                other_user.last_seen            AS other_last_seen,
                last_msg.content                AS last_content,
                last_msg.sent_at                AS last_time,
                COALESCE(unread.cnt, 0)         AS unread_count
            FROM conversations c
            /* Resolve the "other" user */
            JOIN users other_user
                 ON other_user.id = IF(c.user_one_id = ?, c.user_two_id, c.user_one_id)
            /* Latest message in the conversation (subquery) */
            LEFT JOIN (
                SELECT m1.conversation_id, m1.content, m1.sent_at
                FROM messages m1
                INNER JOIN (
                    SELECT conversation_id, MAX(sent_at) AS max_sent
                    FROM messages
                    GROUP BY conversation_id
                ) m2 ON m1.conversation_id = m2.conversation_id AND m1.sent_at = m2.max_sent
            ) last_msg ON last_msg.conversation_id = c.id
            /* Unread count: messages sent by the other user that I haven't read */
            LEFT JOIN (
                SELECT conversation_id, COUNT(*) AS cnt
                FROM messages
                WHERE sender_id != ? AND is_read = FALSE
                GROUP BY conversation_id
            ) unread ON unread.conversation_id = c.id
            WHERE c.user_one_id = ? OR c.user_two_id = ?
            ORDER BY last_msg.sent_at DESC, c.created_at DESC
            """;

        return jdbc.query(sql, (ResultSet rs, int rowNum) -> {
            ConversationSummary s = new ConversationSummary();
            s.setConversationId(rs.getLong("conv_id"));
            s.setOtherUserId(rs.getLong("other_id"));
            s.setOtherUsername(rs.getString("other_username"));
            s.setOtherUserOnline(rs.getBoolean("other_online"));
            Timestamp ls = rs.getTimestamp("other_last_seen");
            s.setOtherUserLastSeen(ls != null ? ls.toLocalDateTime() : null);
            s.setLastMessageContent(rs.getString("last_content"));
            Timestamp lt = rs.getTimestamp("last_time");
            s.setLastMessageTime(lt != null ? lt.toLocalDateTime() : null);
            s.setUnreadCount(rs.getInt("unread_count"));
            return s;
        }, userId, userId, userId, userId);
    }
}
