package com.example.franchiseapi.exception;

import com.example.franchiseapi.support.ProcedureFailures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoredProcedureErrors")
class StoredProcedureErrorsTest {

    @Test
    @DisplayName("returns the procedure result when the call succeeds")
    void returnsResult() {
        assertThat(StoredProcedureErrors.call(() -> "ok")).isEqualTo("ok");
    }

    @Test
    @DisplayName("maps error 50404 to ResourceNotFoundException with the procedure message")
    void mapsNotFound() {
        assertThatThrownBy(() -> StoredProcedureErrors.call(() -> {
            throw ProcedureFailures.notFound("Branch with id 10 not found");
        }))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Branch with id 10 not found");
    }

    @Test
    @DisplayName("maps error 50409 to a 409 BusinessException")
    void mapsConflict() {
        assertThatThrownBy(() -> StoredProcedureErrors.run(() -> {
            throw ProcedureFailures.conflict("A franchise named 'A' already exists");
        }))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A franchise named 'A' already exists")
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("maps error 50400 to a 400 BusinessException")
    void mapsBadRequest() {
        assertThatThrownBy(() -> StoredProcedureErrors.call(() -> {
            throw ProcedureFailures.badRequest("Stock must be greater than or equal to 0");
        }))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("finds the server error when Spring JDBC wraps it directly")
    void mapsDirectlyWrappedError() {
        DataAccessException wrapped = new UncategorizedSQLException(
                "CALL", "CALL sp_product_delete(1, 2)",
                new SQLException("Product with id 2 not found in branch 1", "45000", 50404));

        assertThatThrownBy(() -> StoredProcedureErrors.run(() -> {
            throw wrapped;
        }))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 2 not found in branch 1");
    }

    @Test
    @DisplayName("rethrows any other database error untouched")
    void rethrowsOtherErrors() {
        DataIntegrityViolationException duplicateKey = new DataIntegrityViolationException(
                "Duplicate entry", new SQLException("Duplicate entry", "23000", 1062));

        assertThatThrownBy(() -> StoredProcedureErrors.call(() -> {
            throw duplicateKey;
        })).isSameAs(duplicateKey);
    }
}
