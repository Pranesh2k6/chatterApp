package com.example.chat.service;

import com.example.chat.model.Conversation;
import com.example.chat.model.ConversationSummary;
import com.example.chat.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for conversations (find-or-create, access control, sidebar).
 */
@Service
public class ConversationService {

    private final ConversationRepository conversationRepo;

    public ConversationService(ConversationRepository conversationRepo) {
        this.conversationRepo = conversationRepo;
    }

    /**
     * Find the existing conversation between two users, or create a new one.
     * Returns the conversation.
     */
    public Conversation findOrCreate(Long userA, Long userB) {
        Optional<Conversation> existing = conversationRepo.findByUsers(userA, userB);
        if (existing.isPresent()) {
            return existing.get();
        }
        conversationRepo.create(userA, userB);
        // Re-fetch to get the generated id
        return conversationRepo.findByUsers(userA, userB)
                .orElseThrow(() -> new IllegalStateException("Conversation creation failed"));
    }

    public Optional<Conversation> findById(Long id) {
        return conversationRepo.findById(id);
    }

    /**
     * Check that a user is a participant in a conversation.
     */
    public boolean isParticipant(Conversation conv, Long userId) {
        return conv.getUserOneId().equals(userId) || conv.getUserTwoId().equals(userId);
    }

    /**
     * Get the other user's id in a conversation.
     */
    public Long getOtherUserId(Conversation conv, Long myId) {
        return conv.getUserOneId().equals(myId) ? conv.getUserTwoId() : conv.getUserOneId();
    }

    /**
     * Sidebar data: all conversations for a user with latest message and unread count.
     */
    public List<ConversationSummary> getSidebar(Long userId) {
        return conversationRepo.findSummariesForUser(userId);
    }
}
