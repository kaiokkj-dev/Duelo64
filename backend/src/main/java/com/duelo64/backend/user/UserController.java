package com.duelo64.backend.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.auth.AuthUserResponse;
import com.duelo64.backend.shared.ratelimit.ApiRateLimiter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

        private final UserRepository userRepository;
        private final UserService userService;
        private final ApiRateLimiter apiRateLimiter;
        private final PublicProfileService publicProfileService;

        public UserController(
                        UserRepository userRepository,
                        UserService userService,
                        ApiRateLimiter apiRateLimiter,
                        PublicProfileService publicProfileService) {

                this.userRepository = userRepository;
                this.userService = userService;
                this.apiRateLimiter = apiRateLimiter;
                this.publicProfileService = publicProfileService;
        }

        @GetMapping("/{id}/public-profile")
        public ResponseEntity<PublicProfileResponse> getPublicProfile(
                        @PathVariable UUID id,
                        @RequestParam(defaultValue = "CHECKERS") com.duelo64.backend.game.room.GameType gameType) {
                return ResponseEntity.ok(publicProfileService.findById(id, gameType));
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

        @GetMapping("/nicknames/{nickname}/availability")
        public ResponseEntity<NicknameAvailabilityResponse> checkNicknameAvailability(
                        @PathVariable String nickname) {

                String normalizedNickname = nickname.trim();

                if (!normalizedNickname.matches("^[A-Za-z0-9_]{3,24}$")) {
                        return ResponseEntity.ok(
                                        new NicknameAvailabilityResponse(
                                                        normalizedNickname,
                                                        false));
                }

                boolean available = !userRepository
                                .existsByNicknameIgnoreCase(normalizedNickname);

                return ResponseEntity.ok(
                                new NicknameAvailabilityResponse(
                                                normalizedNickname,
                                                available));
        }

        @PatchMapping("/me/profile")
        public ResponseEntity<AuthUserResponse> updateProfile(
                        @AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody UpdateProfileRequest request) {

                UUID userId = UUID.fromString(jwt.getSubject());

                apiRateLimiter.checkProfileUpdate(userId);

                User user = userService.updateProfile(
                                userId,
                                request.nickname(),
                                request.avatarUrl());

                return ResponseEntity.ok(
                                AuthUserResponse.from(user));
        }

        @PostMapping(
                        value = "/me/avatar",
                        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<AuthUserResponse> uploadAvatar(
                        @AuthenticationPrincipal Jwt jwt,
                        @RequestPart("avatar") MultipartFile avatar) {

                UUID userId = UUID.fromString(jwt.getSubject());

                apiRateLimiter.checkAvatarUpload(userId);

                User user = userService.updateAvatar(userId, avatar);

                return ResponseEntity.ok(
                                AuthUserResponse.from(user));
        }
}
