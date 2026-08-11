package com.duelo64.backend.game.chess.api;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.duelo64.backend.game.chess.application.ChessGameService;
import com.duelo64.backend.game.chess.domain.ChessPosition;
import com.duelo64.backend.game.chess.persistence.ChessGameState;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/chess/rooms/{code}/state")
public class ChessGameController {
    private final ChessGameService service;
    public ChessGameController(ChessGameService service) { this.service = service; }

    @GetMapping public ChessGameStateResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return ChessGameStateResponse.from(service.getState(userId(jwt), code));
    }
    @PostMapping("/moves") public ChessGameStateResponse move(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
            @Valid @RequestBody ChessMoveRequest request) {
        return ChessGameStateResponse.from(service.move(userId(jwt), code,
                new ChessPosition(request.fromRow(), request.fromColumn()),
                new ChessPosition(request.toRow(), request.toColumn()), request.promotion()));
    }
    @GetMapping("/legal-moves") public ChessLegalMovesResponse legal(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
            @RequestParam int row, @RequestParam int column) {
        return new ChessLegalMovesResponse(service.legalMoves(userId(jwt), code, new ChessPosition(row, column))
                .stream().map(ChessLegalMoveResponse::from).toList());
    }
    @PostMapping("/resign") public ChessGameStateResponse resign(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return response(service.resign(userId(jwt), code)); }
    @PostMapping("/timeout") public ChessGameStateResponse timeout(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return response(service.confirmTimeout(userId(jwt), code)); }
    @PostMapping("/draw-offer") public ChessGameStateResponse offer(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return response(service.offerDraw(userId(jwt), code)); }
    @PostMapping("/draw-accept") public ChessGameStateResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return response(service.acceptDraw(userId(jwt), code)); }
    @PostMapping("/draw-decline") public ChessGameStateResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return response(service.declineDraw(userId(jwt), code)); }
    private ChessGameStateResponse response(ChessGameState state) { return ChessGameStateResponse.from(state); }
    private UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
