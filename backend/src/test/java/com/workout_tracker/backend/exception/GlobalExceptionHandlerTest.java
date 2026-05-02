package com.workout_tracker.backend.exception;

import com.workout_tracker.backend.dto.common.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

// The duplicate-setNumber pre-check in SetLogService catches sequential cases,
// but a TOCTOU race on concurrent inserts can still trip the DB unique
// constraint. This test pins the handler contract: 409 + canonical envelope,
// not the catch-all 500.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrity_returns409_withCanonicalEnvelope() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; constraint [uk_setlog_exerciselog_setnumber]");

        ResponseEntity<ErrorResponse> resp = handler.handleDataIntegrity(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.error()).isEqualTo("Conflict");
        assertThat(body.message()).contains("uniqueness");
        assertThat(body.timestamp()).isNotNull();
    }
}
