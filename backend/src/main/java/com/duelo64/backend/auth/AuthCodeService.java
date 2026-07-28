package com.duelo64.backend.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duelo64.backend.user.User;
import com.duelo64.backend.user.UserRepository;

@Service
public class AuthCodeService {

        private static final int CODE_BOUND = 1_000_000;
        private static final int CODE_DURATION_MINUTES = 5;
        private static final int MAXIMUM_ATTEMPTS = 5;

        private final AuthCodeRepository authCodeRepository;
        private final AuthEmailService authEmailService;
        private final PasswordEncoder passwordEncoder;
        private final SecureRandom secureRandom;
        private final UserRepository userRepository;

        public AuthCodeService(
                        AuthCodeRepository authCodeRepository,
                        AuthEmailService authEmailService,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {

                this.authCodeRepository = authCodeRepository;
                this.authEmailService = authEmailService;
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.secureRandom = new SecureRandom();
        }

        @Transactional
        public void createCode(String email) {
                String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
                String code = generateSixDigitCode();
                String codeHash = passwordEncoder.encode(code);

                Instant expiresAt = Instant.now()
                                .plus(CODE_DURATION_MINUTES, ChronoUnit.MINUTES);

                AuthCode authCode = new AuthCode(
                                normalizedEmail,
                                codeHash,
                                expiresAt);

                authCodeRepository.save(authCode);
                authEmailService.sendCode(normalizedEmail, code);
        }

        @Transactional(noRollbackFor = InvalidAuthCodeException.class)
        public User verifyCode(String email, String code) {
                String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

                AuthCode authCode = authCodeRepository
                                .findFirstByEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(
                                                normalizedEmail)
                                .orElseThrow(() -> new InvalidAuthCodeException(
                                                "Código inválido ou expirado."));

                if (authCode.isExpired()) {
                        throw new InvalidAuthCodeException(
                                        "Código inválido ou expirado.");
                }

                if (authCode.hasReachedAttemptLimit(MAXIMUM_ATTEMPTS)) {
                        throw new InvalidAuthCodeException(
                                        "Limite de tentativas atingido.");
                }

                if (!passwordEncoder.matches(code, authCode.getCodeHash())) {
                        authCode.registerFailedAttempt();

                        throw new InvalidAuthCodeException(
                                        "Código inválido ou expirado.");
                }

                authCode.markAsUsed();

                User user = userRepository
                                .findByEmailIgnoreCase(normalizedEmail)
                                .orElseGet(() -> new User(normalizedEmail));

                user.registerLogin();
                userRepository.save(user);
                return user;
        }

        private String generateSixDigitCode() {
                int number = secureRandom.nextInt(CODE_BOUND);

                return String.format("%06d", number);
        }
        
}
