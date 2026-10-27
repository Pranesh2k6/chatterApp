package com.example.chat.controller;

import com.example.chat.dto.RegistrationForm;
import com.example.chat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Handles /login and /register pages.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ── Login page ─────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ── Registration ───────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("form") RegistrationForm form,
                                 BindingResult bindingResult,
                                 Model model) {
        // Cross-field validation: passwords must match
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }

        // If bean validation or cross-field check failed, re-show form
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // Try to register
        String error = userService.register(form.getUsername(), form.getEmail(), form.getPassword());
        if (!error.isEmpty()) {
            model.addAttribute("registrationError", error);
            return "register";
        }

        // Success → redirect to login with a flash message
        return "redirect:/login?registered=true";
    }
}
