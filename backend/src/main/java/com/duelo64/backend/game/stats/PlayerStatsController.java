package com.duelo64.backend.game.stats;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duelo64.backend.game.room.GameType;

@RestController
@RequestMapping("/api/v1/stats")
public class PlayerStatsController {
    private final PlayerStatsService service;

    public PlayerStatsController(PlayerStatsService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public PlayerStatsResponse mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "CHECKERS") GameType gameType) {
        return service.statsFor(UUID.fromString(jwt.getSubject()), gameType);
    }
}
