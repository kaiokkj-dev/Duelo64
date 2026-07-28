package com.duelo64.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duelo64.backend.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthCodeService authCodeService;
    private final JwtService jwtService;

    public AuthController(
            AuthCodeService authCodeService,
            JwtService jwtService) {

        this.authCodeService = authCodeService;
        this.jwtService = jwtService;
    }

    @PostMapping("/codes/verify")
    public ResponseEntity<AuthResponse> verifyCode(
            @Valid @RequestBody VerifyAuthCodeRequest request) {

        User user = authCodeService.verifyCode(
                request.email(),
                request.code());

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                AuthUserResponse.from(user));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/codes")
    public ResponseEntity<Void> requestCode(
            @Valid @RequestBody RequestAuthCodeRequest request) {

        authCodeService.createCode(request.email());

        return ResponseEntity.accepted().build();
    }
}