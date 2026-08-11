package com.duelo64.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.duelo64.backend.user.UserRepository;

class AuthCodeServiceTest {

    @Test
    void generatedCodeIsPersistedAsHashAndSentToNormalizedEmail() {
        AuthCodeRepository authCodeRepository = mock(AuthCodeRepository.class);
        AuthEmailService authEmailService = mock(AuthEmailService.class);
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ArgumentCaptor<String> generatedCode = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AuthCode> savedAuthCode = ArgumentCaptor.forClass(AuthCode.class);

        when(passwordEncoder.encode(anyString())).thenReturn("stored-hash");

        AuthCodeService service = new AuthCodeService(
                authCodeRepository,
                authEmailService,
                userRepository,
                passwordEncoder);

        service.createCode("  Jogador@Example.com ");

        verify(passwordEncoder).encode(generatedCode.capture());
        verify(authCodeRepository).save(savedAuthCode.capture());
        verify(authEmailService).sendCode("jogador@example.com", generatedCode.getValue());
        assertThat(generatedCode.getValue()).matches("\\d{6}");
        assertThat(savedAuthCode.getValue().getEmail()).isEqualTo("jogador@example.com");
        assertThat(savedAuthCode.getValue().getCodeHash()).isEqualTo("stored-hash");
    }
}
