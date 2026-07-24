package com.duelo64.backend.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthCodeRepository extends JpaRepository<AuthCode, UUID> {

    Optional<AuthCode> findFirstByEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(
        String email
    );
}