package com.duelo64.backend.game.room;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.checkers.application.CheckersGameService;
import com.duelo64.backend.game.checkers.persistence.CheckersGameStateRepository;
import com.duelo64.backend.user.User;
import com.duelo64.backend.user.UserRepository;

@Service
public class GameRoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_ATTEMPTS = 20;

    private final GameRoomRepository gameRoomRepository;
    private final UserRepository userRepository;
    private final CheckersGameService checkersGameService;
    private final CheckersGameStateRepository checkersGameStateRepository;
    private final RoomRealtimePublisher roomRealtimePublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public GameRoomService(
            GameRoomRepository gameRoomRepository,
            UserRepository userRepository,
            CheckersGameService checkersGameService,
            CheckersGameStateRepository checkersGameStateRepository,
            RoomRealtimePublisher roomRealtimePublisher) {

        this.gameRoomRepository = gameRoomRepository;
        this.userRepository = userRepository;
        this.checkersGameService = checkersGameService;
        this.checkersGameStateRepository = checkersGameStateRepository;
        this.roomRealtimePublisher = roomRealtimePublisher;
    }

    @Transactional
    public GameRoom createPrivateCheckersRoom(UUID userId, int timeControlMinutes) {
        User host = findUser(userId);
        String code = generateUniqueCode();
        GameRoom room = GameRoom.privateCheckers(code, timeControlMinutes, host);
        GameRoom savedRoom = gameRoomRepository.save(room);
        checkersGameService.createInitialState(savedRoom);
        roomRealtimePublisher.publish(RoomRealtimeEvent.roomCreated(savedRoom));

        return savedRoom;
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
        checkersGameStateRepository
                .findByRoomId(room.getId())
                .ifPresent(state -> state.startClock(room.getStartedAt()));
        roomRealtimePublisher.publish(RoomRealtimeEvent.playerJoined(room));

        return room;
    }

    @Transactional(readOnly = true)
    public GameRoom getRoom(String code) {
        return findByCode(code);
    }

    private GameRoom findByCode(String code) {
        return gameRoomRepository
                .findByCode(normalizeCode(code))
                .orElseThrow(RoomNotFoundException::new);
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario nao encontrado."));
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
