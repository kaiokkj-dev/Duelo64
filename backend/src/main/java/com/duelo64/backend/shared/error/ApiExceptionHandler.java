package com.duelo64.backend.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.auth.InvalidAuthCodeException;
import com.duelo64.backend.game.checkers.domain.InvalidCheckersMoveException;
import com.duelo64.backend.game.chess.domain.InvalidChessMoveException;
import com.duelo64.backend.game.room.RoomNotFoundException;
import com.duelo64.backend.game.room.RoomUnavailableException;
import com.duelo64.backend.user.AvatarUploadException;
import com.duelo64.backend.user.InvalidAvatarException;
import com.duelo64.backend.user.NicknameUnavailableException;
import com.duelo64.backend.shared.ratelimit.RateLimitException;

@RestControllerAdvice
public class ApiExceptionHandler {

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiErrorResponse> handleResponseStatus(
                        ResponseStatusException exception) {

                String message = exception.getReason() == null || exception.getReason().isBlank()
                                ? "Nao foi possivel concluir a acao."
                                : exception.getReason();
                ApiErrorResponse response = new ApiErrorResponse(
                                "HTTP_" + exception.getStatusCode().value(),
                                message);

                return ResponseEntity
                                .status(exception.getStatusCode())
                                .body(response);
        }

        @ExceptionHandler(RateLimitException.class)
        public ResponseEntity<ApiErrorResponse> handleRateLimit(
                        RateLimitException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "RATE_LIMIT_EXCEEDED",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.TOO_MANY_REQUESTS)
                                .header(
                                                HttpHeaders.RETRY_AFTER,
                                                Long.toString(exception.getRetryAfterSeconds()))
                                .body(response);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
                        MaxUploadSizeExceededException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "AVATAR_TOO_LARGE",
                                "A imagem deve possuir no máximo 2 MB.");

                return ResponseEntity
                                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                                .body(response);
        }

        @ExceptionHandler(InvalidAvatarException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidAvatar(
                        InvalidAvatarException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "INVALID_AVATAR",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(AvatarUploadException.class)
        public ResponseEntity<ApiErrorResponse> handleAvatarUpload(
                        AvatarUploadException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "AVATAR_UPLOAD_FAILED",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_GATEWAY)
                                .body(response);
        }

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

        @ExceptionHandler(RoomNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleRoomNotFound(
                        RoomNotFoundException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "ROOM_NOT_FOUND",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(response);
        }

        @ExceptionHandler(RoomUnavailableException.class)
        public ResponseEntity<ApiErrorResponse> handleRoomUnavailable(
                        RoomUnavailableException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "ROOM_UNAVAILABLE",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(InvalidCheckersMoveException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidCheckersMove(
                        InvalidCheckersMoveException exception) {

                ApiErrorResponse response = new ApiErrorResponse(
                                "INVALID_CHECKERS_MOVE",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(InvalidChessMoveException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidChessMove(
                        InvalidChessMoveException exception) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new ApiErrorResponse("INVALID_CHESS_MOVE", exception.getMessage()));
        }
}
