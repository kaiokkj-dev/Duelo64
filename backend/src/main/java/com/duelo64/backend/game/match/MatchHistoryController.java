package com.duelo64.backend.game.match;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.duelo64.backend.game.room.GameType;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchHistoryController {
    private final MatchHistoryService service;
    public MatchHistoryController(MatchHistoryService service) { this.service = service; }

    @GetMapping("/me")
    public List<MatchHistoryItemResponse> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) GameType gameType) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var matches = gameType == null ? service.historyFor(userId) : service.historyFor(userId, gameType);
        return matches.stream().map(match -> MatchHistoryItemResponse.from(match, userId)).toList();
    }
}
