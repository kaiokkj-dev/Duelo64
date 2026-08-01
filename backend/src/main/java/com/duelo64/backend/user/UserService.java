package com.duelo64.backend.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User updateProfile(UUID userId, String nickname) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuário não encontrado."
                        )
                );

        String normalizedNickname = nickname.trim();

        boolean nicknameInUse = userRepository
                .existsByNicknameIgnoreCaseAndIdNot(
                        normalizedNickname,
                        userId
                );

        if (nicknameInUse) {
            throw new NicknameUnavailableException();
        }

        user.updateNickname(normalizedNickname);

        return user;
    }
}