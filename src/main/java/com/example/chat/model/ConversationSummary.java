package com.example.chat.model;

import java.time.LocalDateTime;

/**
 * A lightweight projection used by the sidebar to display each conversation.
 * Populated via a complex JOIN query in ConversationRepository.
 */
public class ConversationSummary {

    private Long conversationId;
    private Long otherUserId;
    private String otherUsername;
    private boolean otherUserOnline;
    private LocalDateTime otherUserLastSeen;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private int unreadCount;

    public ConversationSummary() {}

    // ── Getters & setters ──────────────────────────────────

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }

    public boolean isOtherUserOnline() { return otherUserOnline; }
    public void setOtherUserOnline(boolean otherUserOnline) { this.otherUserOnline = otherUserOnline; }

    public LocalDateTime getOtherUserLastSeen() { return otherUserLastSeen; }
    public void setOtherUserLastSeen(LocalDateTime otherUserLastSeen) { this.otherUserLastSeen = otherUserLastSeen; }

    public String getLastMessageContent() { return lastMessageContent; }
    public void setLastMessageContent(String lastMessageContent) { this.lastMessageContent = lastMessageContent; }

    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
