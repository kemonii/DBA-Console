package una.dbadmin.files;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates the names of backup files before they are resolved against a directory object's path. Only plain
 * file names with a {@code .dmp} or {@code .log} extension are accepted, so a request can never address a file
 * outside the directory through separators, drive letters, or {@code ..} segments.
 */
final class BackupFileNames {

    private static final int MAX_LENGTH = 255;

    /** Letters, digits, underscore, dot and hyphen; the first character cannot be a dot or a hyphen. */
    private static final Pattern PLAIN_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.\\-]*");

    /** Device names that Windows resolves to hardware, with or without an extension. */
    private static final Pattern WINDOWS_RESERVED =
            Pattern.compile("(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?", Pattern.CASE_INSENSITIVE);

    private BackupFileNames() {
    }

    /**
     * Returns the name unchanged when it is a plain {@code .dmp} or {@code .log} file name.
     *
     * @throws IllegalArgumentException if the name is blank, too long, contains separators or {@code ..}, uses
     *                                  characters outside the allowed set, or has another extension
     */
    static String requireValid(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        if (!isValid(name)) {
            throw new IllegalArgumentException(
                    "Invalid file name: only plain names ending in .dmp or .log are allowed");
        }
        return name;
    }

    static boolean isValid(String name) {
        return name != null
                && name.length() <= MAX_LENGTH
                && PLAIN_NAME.matcher(name).matches()
                && !name.contains("..")
                && !WINDOWS_RESERVED.matcher(name).matches()
                && hasBackupExtension(name);
    }

    static boolean isLog(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".log");
    }

    private static boolean hasBackupExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return (lower.endsWith(".dmp") && lower.length() > ".dmp".length())
                || (lower.endsWith(".log") && lower.length() > ".log".length());
    }
}
