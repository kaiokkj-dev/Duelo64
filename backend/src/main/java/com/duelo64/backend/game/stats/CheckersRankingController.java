package com.duelo64.backend.game.stats;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duelo64.backend.game.room.GameType;

@RestController
@RequestMapping("/api/v1/rankings")
public class CheckersRankingController {
    private final CheckersRankingService service;

    public CheckersRankingController(CheckersRankingService service) {
        this.service = service;
    }

    @GetMapping("/{game:checkers|chess}")
    public CheckersRankingPageResponse ranking(
            @PathVariable String game,
            @RequestParam(defaultValue = "0") int page) {
        return service.ranking(gameType(game), page);
    }

    @GetMapping("/{game:checkers|chess}/me")
    public CheckersRankingMeResponse mine(@PathVariable String game, @AuthenticationPrincipal Jwt jwt) {
        return service.positionFor(gameType(game), UUID.fromString(jwt.getSubject()));
    }

    private GameType gameType(String game) {
        return GameType.valueOf(game.toUpperCase(java.util.Locale.ROOT));
    }
}
