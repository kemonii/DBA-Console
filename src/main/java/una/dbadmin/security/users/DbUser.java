package una.dbadmin.security.users;

import java.time.LocalDateTime;

/**
 * A database user created by an administrator, as listed in {@code DBA_USERS}.
 *
 * @param username            user name
 * @param accountStatus       {@code OPEN}, {@code LOCKED}, {@code EXPIRED}, ...
 * @param defaultTablespace   tablespace where the user's objects are created by default
 * @param temporaryTablespace tablespace used for sorts
 * @param profile             profile that limits the user's resources and passwords
 * @param created             creation date
 */
public record DbUser(String username, String accountStatus, String defaultTablespace, String temporaryTablespace,
                     String profile, LocalDateTime created) {
}
