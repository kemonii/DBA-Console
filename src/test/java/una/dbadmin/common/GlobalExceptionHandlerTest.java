package una.dbadmin.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentMapsToBadRequest() {
        ResponseEntity<OperationResult> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Invalid tablespace name"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        OperationResult body = assertBody(response);
        assertFalse(body.ok());
        assertEquals("Invalid tablespace name", body.message());
        assertNull(body.data());
    }

    @Test
    void dataAccessMapsToInternalServerErrorAndKeepsOracleCode() {
        SQLException cause = new SQLException(
                "ORA-00942: table or view does not exist\n\nhttps://docs.oracle.com/error-help/db/ora-00942/",
                "42000", 942);

        ResponseEntity<OperationResult> response =
                handler.handleDataAccess(new BadSqlGrammarException("query", "SELECT * FROM missing", cause));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        OperationResult body = assertBody(response);
        assertFalse(body.ok());
        assertEquals("ORA-00942: table or view does not exist", body.message());
    }

    @Test
    void connectionFailureKeepsNestedOracleCode() {
        SQLException wrapper = new SQLException("Connection is not available, request timed out",
                new SQLRecoverableException("ORA-17002: I/O error: Connection refused"));

        ResponseEntity<OperationResult> response = handler.handleDataAccess(
                new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", wrapper));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("ORA-17002: I/O error: Connection refused", assertBody(response).message());
    }

    @Test
    void dataAccessWithoutOracleCodeUsesCauseMessage() {
        ResponseEntity<OperationResult> response =
                handler.handleDataAccess(new DataAccessResourceFailureException("Connection refused"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        OperationResult body = assertBody(response);
        assertFalse(body.ok());
        assertEquals("Database error: Connection refused", body.message());
    }

    private static OperationResult assertBody(ResponseEntity<OperationResult> response) {
        OperationResult body = response.getBody();
        assertNotNull(body);
        return body;
    }
}
