package una.dbadmin.security.auditing;

/**
 * Which outcomes an audit option records: the {@code WHENEVER} clause of {@code AUDIT} and {@code NOAUDIT}.
 */
public enum Whenever {

    /** Successful and failed attempts (no {@code WHENEVER} clause). */
    ALWAYS(""),
    SUCCESSFUL(" WHENEVER SUCCESSFUL"),
    NOT_SUCCESSFUL(" WHENEVER NOT SUCCESSFUL");

    private final String clause;

    Whenever(String clause) {
        this.clause = clause;
    }

    /** The clause to append to the statement, with a leading space, or an empty string for {@link #ALWAYS}. */
    String clause() {
        return clause;
    }
}
