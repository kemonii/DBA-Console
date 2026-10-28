package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FlashbackServiceTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 26, 10, 15, 0);

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FlashbackService service = new FlashbackService(jdbc);

    @Test
    void restoresADroppedTableFromTheRecycleBin() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("HR"), eq("EMPLOYEES"))).thenReturn(1);

        service.restoreTable(new FlashbackTableRequest("hr", "employees"));

        verify(jdbc).execute("FLASHBACK TABLE HR.EMPLOYEES TO BEFORE DROP");
    }

    @Test
    void refusesWhenTheTableIsNotInTheRecycleBin() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("HR"), eq("EMPLOYEES"))).thenReturn(0);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.restoreTable(new FlashbackTableRequest("hr", "employees")));

        assertEquals("Table HR.EMPLOYEES is not in the recycle bin", error.getMessage());
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void queriesTheTableAsOfATimestampWithABindValue() {
        List<Map<String, Object>> rows = List.of(Map.of("ID", 1));
        when(jdbc.queryForList(anyString(), eq(Timestamp.valueOf(AT)))).thenReturn(rows);

        List<Map<String, Object>> result = service.queryAsOf(new FlashbackQueryRequest("hr", "employees", AT));

        assertEquals(rows, result);
        verify(jdbc).queryForList(
                "SELECT * FROM HR.EMPLOYEES AS OF TIMESTAMP ? FETCH FIRST 100 ROWS ONLY", Timestamp.valueOf(AT));
    }

    @Test
    void restoresOnlyTheRowsMissingNow() {
        when(jdbc.update(anyString(), eq(Timestamp.valueOf(AT)))).thenReturn(3);

        int restored = service.restoreRows(new FlashbackQueryRequest("hr", "employees", AT));

        assertEquals(3, restored);
        verify(jdbc).update("INSERT INTO HR.EMPLOYEES SELECT * FROM HR.EMPLOYEES AS OF TIMESTAMP ? "
                + "MINUS SELECT * FROM HR.EMPLOYEES", Timestamp.valueOf(AT));
    }

    @Test
    void invalidNamesNeverReachTheDatabase() {
        assertThrows(IllegalArgumentException.class,
                () -> service.restoreTable(new FlashbackTableRequest("hr; drop user x", "employees")));
        assertThrows(IllegalArgumentException.class,
                () -> service.queryAsOf(new FlashbackQueryRequest("hr", "emp loyees", AT)));
        assertThrows(IllegalArgumentException.class,
                () -> service.restoreRows(new FlashbackQueryRequest("hr", "x--", AT)));

        verifyNoInteractions(jdbc);
    }

    @Test
    void timestampIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> service.queryAsOf(new FlashbackQueryRequest("hr", "employees", null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.restoreRows(new FlashbackQueryRequest("hr", "employees", null)));

        verifyNoInteractions(jdbc);
    }
}
