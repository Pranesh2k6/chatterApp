package com.example.chat.service;

import com.example.chat.model.Message;
import com.example.chat.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for sending, loading, and marking messages.
 */
@Service
public class MessageService {

    private final MessageRepository messageRepo;

    public MessageService(MessageRepository messageRepo) {
        this.messageRepo = messageRepo;
    }

    /**
     * Send a message.  Trims content and rejects blanks / over-limit.
     * @return error message, or empty string on success.
     */
    public String send(Long conversationId, Long senderId, String content) {
        if (content == null) {
            return "Message cannot be empty.";
        }
        content = content.trim();
        if (content.isEmpty()) {
            return "Message cannot be empty.";
        }
        if (content.length() > 1000) {
            return "Message cannot exceed 1000 characters.";
        }

        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        messageRepo.save(msg);
        return "";
    }

    /**
     * Load messages for a conversation with a limit.
     * Returns oldest-first for display.
     */
    public List<Message> getMessages(Long conversationId, int limit) {
        return messageRepo.findByConversation(conversationId, limit);
    }

    /** Total message count in a conversation. */
    public int countMessages(Long conversationId) {
        return messageRepo.countByConversation(conversationId);
    }

    /** Mark the other user's messages as read when I open the conversation. */
    public void markRead(Long conversationId, Long otherUserId) {
        messageRepo.markAsRead(conversationId, otherUserId);
    }

    /** Check for un-notified messages (for the notification sound). */
    public boolean hasUnnotified(Long userId) {
        return messageRepo.hasUnnotifiedMessages(userId);
    }

    /** Mark all my incoming un-notified messages as notified. */
    public void markNotified(Long userId) {
        messageRepo.markAsNotified(userId);
    }
}
