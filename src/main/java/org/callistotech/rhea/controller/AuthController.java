package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.RegisterRequest;
import org.callistotech.rhea.model.AppUser;
import org.callistotech.rhea.service.UserService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        String email = request.email() == null ? "" : request.email().trim();
        String password = request.password() == null ? "" : request.password();

        if (name.isEmpty() || email.isEmpty() || password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Name, email, and a password of at least 8 characters are required."));
        }
        try {
            AppUser user = userService.registerLocal(name, email, password);
            return ResponseEntity.ok(Map.of(
                    "registered", true,
                    "email", user.getUserName(),
                    "name", user.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataAccessException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "An account with this email already exists."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("loggedIn", false));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email;
        String name;
        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else {
            email = principal.getName();
            name = email;
        }
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("loggedIn", false));
        }

        AppUser user = userService.findByUserName(email).orElse(null);
        if (user != null && user.getName() != null) {
            name = user.getName();
        }

        return ResponseEntity.ok(Map.of(
                "loggedIn", true,
                "email", email,
                "name", name != null ? name : email));
    }
}
