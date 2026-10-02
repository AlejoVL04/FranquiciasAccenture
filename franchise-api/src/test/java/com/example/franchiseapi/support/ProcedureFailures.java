package com.example.franchiseapi.support;

import com.example.franchiseapi.exception.StoredProcedureErrors;
import org.hibernate.exception.GenericJDBCException;
import org.springframework.orm.jpa.JpaSystemException;

import java.sql.SQLException;

/**
 * Builds the exception a Spring Data repository throws when a stored procedure
 * rejects a call with {@code SIGNAL SQLSTATE '45000'}: the server error,
 * wrapped by Hibernate, translated by Spring.
 */
public final class ProcedureFailures {

    private ProcedureFailures() {
    }

    public static JpaSystemException notFound(String message) {
        return signal(StoredProcedureErrors.NOT_FOUND, message);
    }

    public static JpaSystemException conflict(String message) {
        return signal(StoredProcedureErrors.CONFLICT, message);
    }

    public static JpaSystemException badRequest(String message) {
        return signal(StoredProcedureErrors.BAD_REQUEST, message);
    }

    public static JpaSystemException signal(int errno, String message) {
        SQLException serverError = new SQLException(message, "45000", errno);
        return new JpaSystemException(new GenericJDBCException("could not execute query", serverError));
    }
}
