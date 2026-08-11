package com.duelo64.backend.game.room;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.user.User;
import com.duelo64.backend.user.UserRepository;

@Service
public class GameRoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_ATTEMPTS = 20;

    private final GameRoomRepository gameRoomRepository;
    private final UserRepository userRepository;
    private final List<GameStateLifecycle> gameStateLifecycles;
    private final RoomRealtimePublisher roomRealtimePublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public GameRoomService(
            GameRoomRepository gameRoomRepository,
            UserRepository userRepository,
            List<GameStateLifecycle> gameStateLifecycles,
            RoomRealtimePublisher roomRealtimePublisher) {

        this.gameRoomRepository = gameRoomRepository;
        this.userRepository = userRepository;
        this.gameStateLifecycles = List.copyOf(gameStateLifecycles);
        this.roomRealtimePublisher = roomRealtimePublisher;
    }

    @Transactional
    public GameRoom createPrivateCheckersRoom(UUID userId, int timeControlMinutes) {
        return createPrivateRoom(userId, GameType.CHECKERS, timeControlMinutes);
    }

    @Transactional
    public GameRoom createPrivateRoom(UUID userId, GameType gameType, int timeControlMinutes) {
        User host = findUser(userId);
        String code = generateUniqueCode();
        GameRoom room = GameRoom.privateRoom(code, gameType, timeControlMinutes, host);
        GameRoom savedRoom = gameRoomRepository.save(room);
        lifecycleFor(gameType).initialize(savedRoom);
        roomRealtimePublisher.publish(RoomRealtimeEvent.roomCreated(savedRoom));

        return savedRoom;
    }

    @Transactional
    public GameRoom createRankedCheckersRoom(UUID whiteUserId, UUID blackUserId, int timeControlMinutes) {
        return createRankedRoom(whiteUserId, blackUserId, GameType.CHECKERS, timeControlMinutes);
    }

    @Transactional
    public GameRoom createRankedRoom(
            UUID whiteUserId, UUID blackUserId, GameType gameType, int timeControlMinutes) {
        if (whiteUserId.equals(blackUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Um jogador nao pode enfrentar a si mesmo.");
        }
        User white = findUser(whiteUserId);
        User black = findUser(blackUserId);
        GameRoom room = GameRoom.rankedRoom(generateUniqueCode(), gameType, timeControlMinutes, white);
        gameRoomRepository.save(room);
        room.join(black);
        GameStateLifecycle lifecycle = lifecycleFor(gameType);
        lifecycle.initialize(room);
        lifecycle.start(room);
        return room;
    }

    @Transactional
    public GameRoom joinRoom(UUID userId, String code) {
        User guest = findUser(userId);
        GameRoom room = findByCode(code);

        if (room.getHost().getId().equals(userId)) {
            return room;
        }

        if (room.getGuest() != null && room.getGuest().getId().equals(userId)) {
            return room;
        }

        if (room.getStatus() != RoomStatus.WAITING || room.isFull()) {
            throw new RoomUnavailableException("Esta sala ja esta cheia ou em andamento.");
        }

        room.join(guest);
        lifecycleFor(room.getGameType()).start(room);
        roomRealtimePublisher.publish(RoomRealtimeEvent.playerJoined(room));

        return room;
    }

    @Transactional(readOnly = true)
    public GameRoom getRoom(UUID userId, String code) {
        GameRoom room = findByCode(code);
        requireParticipant(room, userId);
        return room;
    }

    @Transactional
    public List<GameRoom> resolveTimedOutRooms(UUID userId) {
        List<GameRoom> activeRooms = gameRoomRepository
                .findActiveRoomsForUser(userId, RoomStatus.IN_PROGRESS);

        activeRooms.forEach(room -> lifecycleFor(room.getGameType()).resolveTimeout(room, userId));

        return gameRoomRepository.findActiveRoomsForUser(userId, RoomStatus.IN_PROGRESS);
    }

    @Transactional
    public GameRoom requestRematch(UUID userId, String code) {
        GameRoom room = findByCodeForUpdate(code);
        requireFinishedParticipant(room, userId);

        if (room.getRematchRoomCode() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A revanche ja foi criada.");
        }
        if (room.hasPendingRematch()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um pedido de revanche pendente.");
        }

        room.requestRematch(userId);
        roomRealtimePublisher.publish(RoomRealtimeEvent.rematchRequested(room));
        return room;
    }

    @Transactional
    public GameRoom declineRematch(UUID userId, String code) {
        GameRoom room = findByCodeForUpdate(code);
        requireFinishedParticipant(room, userId);
        validateRematchResponder(room, userId);

        room.declineRematch();
        roomRealtimePublisher.publish(RoomRealtimeEvent.rematchDeclined(room));
        return room;
    }

    @Transactional
    public GameRoom acceptRematch(UUID userId, String code) {
        GameRoom originalRoom = findByCodeForUpdate(code);
        requireFinishedParticipant(originalRoom, userId);

        if (originalRoom.getRematchRequestedByUserId() != null
                && originalRoom.getRematchRequestedByUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao pode aceitar o proprio pedido.");
        }
        if (originalRoom.getRematchRoomCode() != null) {
            return findByCode(originalRoom.getRematchRoomCode());
        }
        validateRematchResponder(originalRoom, userId);

        User newHost = originalRoom.getGuest();
        User newGuest = originalRoom.getHost();
        GameRoom rematch = GameRoom.privateRoom(
                generateUniqueCode(),
                originalRoom.getGameType(),
                originalRoom.getTimeControlMinutes(),
                newHost);
        gameRoomRepository.save(rematch);
        rematch.join(newGuest);

        GameStateLifecycle lifecycle = lifecycleFor(rematch.getGameType());
        lifecycle.initialize(rematch);
        lifecycle.start(rematch);
        originalRoom.linkRematch(rematch.getCode());

        roomRealtimePublisher.publish(RoomRealtimeEvent.rematchAccepted(originalRoom, rematch.getCode()));
        return rematch;
    }

    private void requireFinishedParticipant(GameRoom room, UUID userId) {
        requireParticipant(room, userId);
        if (room.getStatus() != RoomStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A revanche so pode ser pedida apos o fim da partida.");
        }
        if (room.getGuest() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A partida nao possui dois jogadores.");
        }
    }

    private void validateRematchResponder(GameRoom room, UUID userId) {
        if (!room.hasPendingRematch()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nao existe pedido de revanche pendente.");
        }
        if (room.getRematchRequestedByUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao pode responder ao proprio pedido.");
        }
    }

    private void requireParticipant(GameRoom room, UUID userId) {
        boolean isHost = room.getHost().getId().equals(userId);
        boolean isGuest = room.getGuest() != null && room.getGuest().getId().equals(userId);

        if (!isHost && !isGuest) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao participa desta sala.");
        }
    }

    private GameRoom findByCode(String code) {
        return gameRoomRepository
                .findByCode(normalizeCode(code))
                .orElseThrow(RoomNotFoundException::new);
    }

    private GameRoom findByCodeForUpdate(String code) {
        return gameRoomRepository
                .findByCodeForUpdate(normalizeCode(code))
                .orElseThrow(RoomNotFoundException::new);
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario nao encontrado."));
    }

    private GameStateLifecycle lifecycleFor(GameType gameType) {
        return gameStateLifecycles.stream()
                .filter(candidate -> candidate.gameType() == gameType)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_IMPLEMENTED,
                        "Esta modalidade ainda nao possui motor de jogo."));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            String code = generateCode();

            if (!gameRoomRepository.existsByCode(code)) {
                return code;
            }
        }

        throw new RoomUnavailableException("Nao foi possivel gerar uma sala agora. Tente novamente.");
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int index = 0; index < CODE_LENGTH; index++) {
            int characterIndex = secureRandom.nextInt(CODE_ALPHABET.length());
            code.append(CODE_ALPHABET.charAt(characterIndex));
        }

        return code.toString();
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }

        return code.trim().toUpperCase(Locale.ROOT);
    }
}
