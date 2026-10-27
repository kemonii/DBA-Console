package una.dbadmin.files;

/**
 * The text of a log file.
 *
 * @param name      file name
 * @param content   the text read, limited to the service's maximum number of lines and characters
 * @param truncated whether the file has more content than was returned
 */
public record LogContent(String name, String content, boolean truncated) {
}
