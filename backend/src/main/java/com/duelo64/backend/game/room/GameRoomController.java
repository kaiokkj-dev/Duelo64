package com.duelo64.backend.game.room;

import java.net.URI;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/checkers/rooms")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    public GameRoomController(GameRoomService gameRoomService) {
        this.gameRoomService = gameRoomService;
    }

    @PostMapping
    public ResponseEntity<GameRoomResponse> createPrivateRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRoomRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        GameRoom room = gameRoomService.createPrivateCheckersRoom(
                userId,
                request.timeControlMinutes());

        return ResponseEntity
                .created(URI.create("/api/v1/checkers/rooms/" + room.getCode()))
                .body(GameRoomResponse.from(room));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<GameRoomResponse> joinRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {

        UUID userId = UUID.fromString(jwt.getSubject());
        GameRoom room = gameRoomService.joinRoom(userId, code);

        return ResponseEntity.ok(GameRoomResponse.from(room));
    }

    @GetMapping("/{code}")
    public ResponseEntity<GameRoomResponse> getRoom(@PathVariable String code) {
        GameRoom room = gameRoomService.getRoom(code);

        return ResponseEntity.ok(GameRoomResponse.from(room));
    }
}
