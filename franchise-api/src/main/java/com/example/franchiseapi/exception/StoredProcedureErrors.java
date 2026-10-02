package com.example.franchiseapi.exception;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Turns the errors raised by the stored procedures into the API's domain
 * exceptions.
 * <p>
 * The business rules live in the procedures ({@code R__stored_procedures.sql}).
 * A procedure rejects a call with {@code SIGNAL SQLSTATE '45000'} and one of the
 * error numbers below, and its message is already the client-facing text. Any
 * other database error, such as a unique-index violation caused by a concurrent
 * write, is rethrown untouched for {@link GlobalExceptionHandler} to handle.
 */
public final class StoredProcedureErrors {

    public static final int BAD_REQUEST = 50400;
    public static final int NOT_FOUND = 50404;
    public static final int CONFLICT = 50409;

    private StoredProcedureErrors() {
    }

    /** Runs a procedure call that returns a result. */
    public static <T> T call(Supplier<T> procedureCall) {
        try {
            return procedureCall.get();
        } catch (DataAccessException ex) {
            throw translate(ex);
        }
    }

    /** Runs a procedure call that returns nothing. */
    public static void run(Runnable procedureCall) {
        try {
            procedureCall.run();
        } catch (DataAccessException ex) {
            throw translate(ex);
        }
    }

    static RuntimeException translate(DataAccessException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                switch (sql.getErrorCode()) {
                    case NOT_FOUND:
                        return new ResourceNotFoundException(sql.getMessage());
                    case CONFLICT:
                        return BusinessException.conflict(sql.getMessage());
                    case BAD_REQUEST:
                        return new BusinessException(sql.getMessage());
                    default:
                        // Keep walking: the driver may wrap the server error.
                }
            }
        }
        return ex;
    }
}
