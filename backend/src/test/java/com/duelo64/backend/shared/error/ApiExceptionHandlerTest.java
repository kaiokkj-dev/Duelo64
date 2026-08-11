package com.duelo64.backend.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.duelo64.backend.game.chess.domain.InvalidChessMoveException;

class ApiExceptionHandlerTest {
    @Test
    void responseStatusReasonIsReturnedToFrontend() {
        var response = new ApiExceptionHandler().handleResponseStatus(
                new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Voce ja esta em uma partida em andamento."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("HTTP_409");
        assertThat(response.getBody().message()).isEqualTo("Voce ja esta em uma partida em andamento.");
    }

    @Test
    void chessDomainMessageIsReturnedWithoutInternalDetails() {
        var response = new ApiExceptionHandler().handleInvalidChessMove(
                new InvalidChessMoveException("A jogada deixa o proprio rei em xeque."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_CHESS_MOVE");
        assertThat(response.getBody().message()).isEqualTo("A jogada deixa o proprio rei em xeque.");
    }
}
