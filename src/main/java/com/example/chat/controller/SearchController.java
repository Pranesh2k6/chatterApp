package com.example.chat.controller;

import com.example.chat.model.User;
import com.example.chat.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Handles user search and displays results.
 */
@Controller
public class SearchController {

    private final UserService userService;

    public SearchController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam(name = "q", required = false) String query,
                             Model model,
                             Principal principal) {
        User me = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));

        List<User> results = Collections.emptyList();
        if (query != null && !query.trim().isEmpty()) {
            results = userService.search(query.trim(), me.getId());
        }

        model.addAttribute("query", query);
        model.addAttribute("results", results);
        return "search";
    }
}
