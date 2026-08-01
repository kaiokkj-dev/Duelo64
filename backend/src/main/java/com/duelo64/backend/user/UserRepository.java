package com.duelo64.backend.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByNicknameIgnoreCaseAndIdNot(
            String nickname,
            UUID id);

    boolean existsByNicknameIgnoreCase(String nickname);
}
