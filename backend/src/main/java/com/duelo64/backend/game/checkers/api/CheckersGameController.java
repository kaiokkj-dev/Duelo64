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
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<CheckersGameStateResponse> getState(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {
        CheckersGameState state = checkersGameService.getState(userId(jwt), code);

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

    @GetMapping("/legal-moves")
    public ResponseEntity<LegalMovesResponse> legalMoves(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @RequestParam int row,
            @RequestParam int column) {

        UUID userId = UUID.fromString(jwt.getSubject());
        var moves = checkersGameService.legalMoves(userId, code, row, column)
                .stream()
                .map(LegalMoveResponse::from)
                .toList();

        return ResponseEntity.ok(new LegalMovesResponse(moves));
    }

    @PostMapping("/resign")
    public ResponseEntity<CheckersGameStateResponse> resign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {
        return stateResponse(checkersGameService.resign(userId(jwt), code));
    }

    @PostMapping("/timeout")
    public ResponseEntity<CheckersGameStateResponse> confirmTimeout(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {
        return stateResponse(checkersGameService.confirmTimeout(userId(jwt), code));
    }

    @PostMapping("/draw-offer")
    public ResponseEntity<CheckersGameStateResponse> offerDraw(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {
        return stateResponse(checkersGameService.offerDraw(userId(jwt), code));
    }

    @PostMapping("/draw-accept")
    public ResponseEntity<CheckersGameStateResponse> acceptDraw(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {
        return stateResponse(checkersGameService.acceptDraw(userId(jwt), code));
    }

    @PostMapping("/draw-decline")
    public ResponseEntity<CheckersGameStateResponse> declineDraw(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {
        return stateResponse(checkersGameService.declineDraw(userId(jwt), code));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private ResponseEntity<CheckersGameStateResponse> stateResponse(CheckersGameState state) {
        return ResponseEntity.ok(CheckersGameStateResponse.from(state));
    }
}
