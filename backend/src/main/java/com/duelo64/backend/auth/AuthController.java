package com.duelo64.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthCodeService authCodeService;

    public AuthController(AuthCodeService authCodeService) {
        this.authCodeService = authCodeService;
    }

    @PostMapping("/codes")
    public ResponseEntity<Void> requestCode(
            @Valid @RequestBody RequestAuthCodeRequest request) {

        authCodeService.createCode(request.email());

        return ResponseEntity.accepted().build();
    }
}