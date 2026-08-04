package com.duelo64.backend.game.checkers.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duelo64.backend.game.checkers.application.CheckersGameService;
import com.duelo64.backend.game.checkers.persistence.CheckersGameState;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/checkers/rooms/{code}/state")
public class CheckersGameController {

    private final CheckersGameService checkersGameService;

    public CheckersGameController(CheckersGameService checkersGameService) {
        this.checkersGameService = checkersGameService;
    }

    @GetMapping
    public ResponseEntity<CheckersGameStateResponse> getState(@PathVariable String code) {
        CheckersGameState state = checkersGameService.getState(code);

        return ResponseEntity.ok(CheckersGameStateResponse.from(state));
    }

    @PostMapping("/moves")
    public ResponseEntity<CheckersGameStateResponse> movePiece(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @Valid @RequestBody MovePieceRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        CheckersGameState state = checkersGameService.movePiece(userId, code, request);

        return ResponseEntity.ok(CheckersGameStateResponse.from(state));
    }
}
