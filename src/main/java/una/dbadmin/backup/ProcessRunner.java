package una.dbadmin.backup;

import java.util.List;

/**
 * Runs an OS process. Exists so services can be tested without Oracle utilities installed.
 */
@FunctionalInterface
public interface ProcessRunner {

    ProcessResult run(List<String> args);
}
