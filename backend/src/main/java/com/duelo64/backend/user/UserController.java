package com.duelo64.backend.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.auth.AuthUserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

        private final UserRepository userRepository;
        private final UserService userService;

        public UserController(
                        UserRepository userRepository,
                        UserService userService) {

                this.userRepository = userRepository;
                this.userService = userService;
        }

        @GetMapping("/me")
        public ResponseEntity<AuthUserResponse> getCurrentUser(
                        @AuthenticationPrincipal Jwt jwt) {

                UUID userId = UUID.fromString(jwt.getSubject());

                User user = userRepository
                                .findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Usuário não encontrado."));

                return ResponseEntity.ok(
                                AuthUserResponse.from(user));
        }

        @PatchMapping("/me/profile")
        public ResponseEntity<AuthUserResponse> updateProfile(
                        @AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody UpdateProfileRequest request) {

                UUID userId = UUID.fromString(jwt.getSubject());

                User user = userService.updateProfile(
                                userId,
                                request.nickname());

                return ResponseEntity.ok(
                                AuthUserResponse.from(user));
        }
}
