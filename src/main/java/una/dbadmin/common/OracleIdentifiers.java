package una.dbadmin.common;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates Oracle object names (users, roles, tablespaces, directories, schemas, tables) before they are
 * concatenated into DDL or utility commands. Identifiers cannot be bind parameters, so this is the only defense
 * against SQL injection for them.
 */
public final class OracleIdentifiers {

    /** Unquoted Oracle identifier: starts with a letter, then letters, digits, _, $ or #, up to 128 characters. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]{0,127}");

    private OracleIdentifiers() {
    }

    /**
     * Returns the name in upper case, as Oracle stores unquoted identifiers in the data dictionary.
     *
     * @throws IllegalArgumentException if the name is not a valid unquoted Oracle identifier
     */
    public static String requireValid(String name) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid Oracle identifier: " + name);
        }
        return name.toUpperCase(Locale.ROOT);
    }
}
