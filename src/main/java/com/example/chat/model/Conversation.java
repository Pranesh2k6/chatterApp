package com.example.chat.model;

import java.time.LocalDateTime;

/**
 * Represents a row in the `conversations` table.
 */
public class Conversation {

    private Long id;
    private Long userOneId;
    private Long userTwoId;
    private LocalDateTime createdAt;

    public Conversation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserOneId() { return userOneId; }
    public void setUserOneId(Long userOneId) { this.userOneId = userOneId; }

    public Long getUserTwoId() { return userTwoId; }
    public void setUserTwoId(Long userTwoId) { this.userTwoId = userTwoId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
