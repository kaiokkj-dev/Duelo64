package com.duelo64.backend.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.duelo64.backend.auth.InvalidAuthCodeException;
import com.duelo64.backend.user.NicknameUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler {

        @ExceptionHandler(InvalidAuthCodeException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidAuthCode(
                        InvalidAuthCodeException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "INVALID_AUTH_CODE",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(response);
        }

        @ExceptionHandler(NicknameUnavailableException.class)
        public ResponseEntity<ApiErrorResponse> handleNicknameUnavailable(
                        NicknameUnavailableException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "NICKNAME_UNAVAILABLE",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }
}
