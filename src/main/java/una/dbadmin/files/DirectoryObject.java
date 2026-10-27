package una.dbadmin.files;

/**
 * An Oracle directory object.
 *
 * @param name name of the directory object, as stored in the data dictionary
 * @param path operating system path the directory object points to
 */
public record DirectoryObject(String name, String path) {
}
