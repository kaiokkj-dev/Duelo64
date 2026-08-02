package com.duelo64.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duelo64.backend.user.User;
import com.duelo64.backend.shared.ratelimit.ApiRateLimiter;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthCodeService authCodeService;
    private final JwtService jwtService;
    private final ApiRateLimiter apiRateLimiter;

    public AuthController(
            AuthCodeService authCodeService,
            JwtService jwtService,
            ApiRateLimiter apiRateLimiter) {

        this.authCodeService = authCodeService;
        this.jwtService = jwtService;
        this.apiRateLimiter = apiRateLimiter;
    }

    @PostMapping("/codes/verify")
    public ResponseEntity<AuthResponse> verifyCode(
            @Valid @RequestBody VerifyAuthCodeRequest request,
            HttpServletRequest httpRequest) {

        apiRateLimiter.checkAuthCodeVerification(
                httpRequest,
                request.email());

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
            @Valid @RequestBody RequestAuthCodeRequest request,
            HttpServletRequest httpRequest) {

        apiRateLimiter.checkAuthCodeRequest(
                httpRequest,
                request.email());

        authCodeService.createCode(request.email());

        return ResponseEntity.accepted().build();
    }
}
