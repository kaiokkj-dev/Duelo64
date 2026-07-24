package com.duelo64.backend.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthCodeService {

    private static final int CODE_BOUND = 1_000_000;
    private static final int CODE_DURATION_MINUTES = 5;

    private final AuthCodeRepository authCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public AuthCodeService(
            AuthCodeRepository authCodeRepository,
            PasswordEncoder passwordEncoder) {

        this.authCodeRepository = authCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    public String createCode(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String code = generateSixDigitCode();
        String codeHash = passwordEncoder.encode(code);

        Instant expiresAt = Instant.now()
                .plus(CODE_DURATION_MINUTES, ChronoUnit.MINUTES);

        AuthCode authCode = new AuthCode(
                normalizedEmail,
                codeHash,
                expiresAt
        );

        authCodeRepository.save(authCode);

        return code;
    }

    private String generateSixDigitCode() {
        int number = secureRandom.nextInt(CODE_BOUND);

        return String.format("%06d", number);
    }
}