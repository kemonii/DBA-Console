package una.dbadmin.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OracleIdentifiersTest {

    @ParameterizedTest
    @ValueSource(strings = {"HR", "ventas", "Ts_Data01", "APP$USER", "DEPT#2"})
    void acceptsValidIdentifiersAndUppercasesThem(String name) {
        assertEquals(name.toUpperCase(), OracleIdentifiers.requireValid(name));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1TABLE", "_HR", "HR DROP", "HR;DROP USER X", "HR'--", "\"HR\"", "A.B"})
    void rejectsInvalidIdentifiers(String name) {
        assertThrows(IllegalArgumentException.class, () -> OracleIdentifiers.requireValid(name));
    }

    @Test
    void acceptsMaximumLength() {
        String name = "A".repeat(128);
        assertEquals(name, OracleIdentifiers.requireValid(name));
    }

    @Test
    void rejectsNamesLongerThanMaximumLength() {
        assertThrows(IllegalArgumentException.class, () -> OracleIdentifiers.requireValid("A".repeat(129)));
    }
}
