package com.example.chat.controller;

import com.example.chat.model.Conversation;
import com.example.chat.model.User;
import com.example.chat.service.ConversationService;
import com.example.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Handles:
 *   GET  /chat            → main chat landing (no conversation selected)
 *   GET  /chat/{id}       → chat page with a specific conversation (iframe host)
 *   POST /chat/start      → find-or-create a conversation, then redirect
 *   POST /chat/{id}/send  → send a message, then redirect back
 */
@Controller
public class ChatController {

    private final UserService userService;
    private final ConversationService conversationService;
    private final com.example.chat.service.MessageService messageService;

    public ChatController(UserService userService,
                          ConversationService conversationService,
                          com.example.chat.service.MessageService messageService) {
        this.userService = userService;
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    // ── Helper: get the currently logged-in User ───────────

    private User currentUser(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));
    }

    // ── Landing page (no conversation selected) ────────────

    @GetMapping("/chat")
    public String chatLanding(Model model, Principal principal) {
        User me = currentUser(principal);
        model.addAttribute("me", me);
        model.addAttribute("conversationId", null);
        model.addAttribute("otherUser", null);
        return "chat";
    }

    // ── Chat page with a conversation selected ─────────────

    @GetMapping("/chat/{id}")
    public String chatPage(@PathVariable("id") Long conversationId,
                           @RequestParam(name = "limit", defaultValue = "50") int limit,
                           Model model,
                           Principal principal) {
        User me = currentUser(principal);

        // Load the conversation and verify access
        Conversation conv = conversationService.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!conversationService.isParticipant(conv, me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Long otherUserId = conversationService.getOtherUserId(conv, me.getId());
        User otherUser = userService.findById(otherUserId)
                .orElseThrow(() -> new IllegalStateException("Other user not found"));

        model.addAttribute("me", me);
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("limit", limit);
        return "chat";
    }

    // ── Start or resume a conversation with another user ───

    @PostMapping("/chat/start")
    public String startConversation(@RequestParam("otherUserId") Long otherUserId,
                                    Principal principal) {
        User me = currentUser(principal);

        // Don't chat with yourself
        if (me.getId().equals(otherUserId)) {
            return "redirect:/search";
        }

        // Verify the other user exists
        userService.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Conversation conv = conversationService.findOrCreate(me.getId(), otherUserId);
        return "redirect:/chat/" + conv.getId();
    }

    // ── Send a message ─────────────────────────────────────

    @PostMapping("/chat/{id}/send")
    public String sendMessage(@PathVariable("id") Long conversationId,
                              @RequestParam("content") String content,
                              @RequestParam(name = "limit", defaultValue = "50") int limit,
                              Principal principal) {
        User me = currentUser(principal);

        Conversation conv = conversationService.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!conversationService.isParticipant(conv, me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Send (validation happens inside the service)
        messageService.send(conversationId, me.getId(), content);

        // Redirect back, preserving the current limit
        return "redirect:/chat/" + conversationId + "?limit=" + limit;
    }
}
