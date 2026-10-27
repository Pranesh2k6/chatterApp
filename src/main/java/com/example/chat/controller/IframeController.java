package com.example.chat.controller;

import com.example.chat.model.*;
import com.example.chat.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

/**
 * Endpoints that render the content of the two iframes on the chat page:
 *
 *   GET /iframe/sidebar             → sidebar with conversation list (refreshes every 3 s)
 *   GET /iframe/messages/{convId}   → message pane for a conversation (refreshes every 2 s)
 *
 * Each returns a minimal HTML page with <meta http-equiv="refresh" ...>.
 */
@Controller
@RequestMapping("/iframe")
public class IframeController {

    private final UserService userService;
    private final ConversationService conversationService;
    private final MessageService messageService;

    public IframeController(UserService userService,
                            ConversationService conversationService,
                            MessageService messageService) {
        this.userService = userService;
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    private User currentUser(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));
    }

    // ── Sidebar iframe ────────────────────────────────────

    @GetMapping("/sidebar")
    public String sidebar(@RequestParam(name = "active", required = false) Long activeConvId,
                          Model model,
                          Principal principal) {
        User me = currentUser(principal);

        List<ConversationSummary> summaries = conversationService.getSidebar(me.getId());
        List<User> onlineUsers = userService.getOnlineUsers(me.getId());

        // Check for un-notified messages (for notification sound)
        boolean playSound = messageService.hasUnnotified(me.getId());
        if (playSound) {
            messageService.markNotified(me.getId());
        }

        model.addAttribute("me", me);
        model.addAttribute("summaries", summaries);
        model.addAttribute("onlineUsers", onlineUsers);
        model.addAttribute("activeConvId", activeConvId);
        model.addAttribute("playSound", playSound);
        return "iframe_sidebar";
    }

    // ── Messages iframe ───────────────────────────────────

    @GetMapping("/messages/{convId}")
    public String messages(@PathVariable("convId") Long convId,
                           @RequestParam(name = "limit", defaultValue = "50") int limit,
                           Model model,
                           Principal principal) {
        User me = currentUser(principal);

        Conversation conv = conversationService.findById(convId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!conversationService.isParticipant(conv, me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Long otherUserId = conversationService.getOtherUserId(conv, me.getId());

        // Mark the other user's messages as read
        messageService.markRead(convId, otherUserId);

        List<Message> messages = messageService.getMessages(convId, limit);
        int totalCount = messageService.countMessages(convId);
        boolean hasOlder = totalCount > limit;

        model.addAttribute("me", me);
        model.addAttribute("convId", convId);
        model.addAttribute("messages", messages);
        model.addAttribute("limit", limit);
        model.addAttribute("nextLimit", limit + 50);
        model.addAttribute("hasOlder", hasOlder);
        return "iframe_messages";
    }
}
