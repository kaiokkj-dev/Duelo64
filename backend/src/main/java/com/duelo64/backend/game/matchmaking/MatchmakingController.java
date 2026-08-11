package com.duelo64.backend.game.matchmaking;

import java.util.UUID;
import java.util.Locale;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import com.duelo64.backend.game.room.GameType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/matchmaking/{game}/queue")
public class MatchmakingController {
    private final MatchmakingService service;

    public MatchmakingController(MatchmakingService service) {
        this.service = service;
    }

    @PostMapping
    public MatchmakingStatusResponse enqueue(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String game,
            @Valid @RequestBody MatchmakingQueueRequest request) {
        return service.enqueue(UUID.fromString(jwt.getSubject()), gameType(game), request.timeControlMinutes());
    }

    @DeleteMapping
    public MatchmakingStatusResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String game) {
        return service.cancel(UUID.fromString(jwt.getSubject()), gameType(game));
    }

    @GetMapping("/status")
    public MatchmakingStatusResponse status(@AuthenticationPrincipal Jwt jwt, @PathVariable String game) {
        return service.status(UUID.fromString(jwt.getSubject()), gameType(game));
    }

    private GameType gameType(String game) {
        try {
            return GameType.valueOf(game.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modalidade nao encontrada.");
        }
    }
}
