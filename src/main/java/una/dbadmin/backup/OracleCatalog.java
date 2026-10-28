package una.dbadmin.backup;

/**
 * Read-only lookups in the data dictionary used to check recovery preconditions.
 */
public interface OracleCatalog {

    boolean tableExists(String schema, String table);

    boolean userExists(String user);
}
