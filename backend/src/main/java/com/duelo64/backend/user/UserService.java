package com.duelo64.backend.user;

import java.util.UUID;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private static final Set<String> ALLOWED_AVATAR_URLS = Set.of(
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Blaze",
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Byte",
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Dash",
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Echo",
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Flux",
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Nova",
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Pixel",
            "https://api.dicebear.com/10.x/bottts-neutral/svg?seed=Volt"
    );

    private final UserRepository userRepository;
    private final AvatarStorageService avatarStorageService;

    public UserService(
            UserRepository userRepository,
            AvatarStorageService avatarStorageService) {

        this.userRepository = userRepository;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional
    public User updateProfile(UUID userId, String nickname, String avatarUrl) {
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

        if (avatarUrl != null) {
            if (!ALLOWED_AVATAR_URLS.contains(avatarUrl)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selecione um avatar válido da galeria."
                );
            }

            user.updateAvatarUrl(avatarUrl);
        }

        return user;
    }

    @Transactional
    public User updateAvatar(UUID userId, MultipartFile avatar) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuário não encontrado."
                        )
                );

        String avatarUrl = avatarStorageService.upload(userId, avatar);
        user.updateAvatarUrl(avatarUrl);

        return user;
    }
}
