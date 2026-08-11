package com.duelo64.backend.game.chess.api;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.duelo64.backend.game.room.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/chess/rooms")
public class ChessRoomController {
    private final GameRoomService service;
    public ChessRoomController(GameRoomService service) { this.service = service; }
    @PostMapping public ResponseEntity<GameRoomResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateRoomRequest request) {
        GameRoom room = service.createPrivateRoom(userId(jwt), GameType.CHESS, request.timeControlMinutes());
        return ResponseEntity.created(URI.create("/api/v1/chess/rooms/" + room.getCode())).body(GameRoomResponse.from(room));
    }
    @PostMapping("/{code}/join") public GameRoomResponse join(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return GameRoomResponse.from(service.joinRoom(userId(jwt), code)); }
    @GetMapping("/{code}") public GameRoomResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return GameRoomResponse.from(service.getRoom(userId(jwt), code)); }
    @PostMapping("/{code}/rematch/request") public GameRoomResponse request(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return GameRoomResponse.from(service.requestRematch(userId(jwt), code)); }
    @PostMapping("/{code}/rematch/decline") public GameRoomResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return GameRoomResponse.from(service.declineRematch(userId(jwt), code)); }
    @PostMapping("/{code}/rematch/accept") public RematchResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) { return new RematchResponse(service.acceptRematch(userId(jwt), code).getCode()); }
    private UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
