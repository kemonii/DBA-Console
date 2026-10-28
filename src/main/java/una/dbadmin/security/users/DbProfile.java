package una.dbadmin.security.users;

import java.util.List;

/**
 * A profile and its limits, as listed in {@code DBA_PROFILES}.
 *
 * @param name   profile name
 * @param limits every resource of the profile, kernel limits first
 */
public record DbProfile(String name, List<Limit> limits) {

    /**
     * One limit of a profile.
     *
     * @param resource     for example {@code SESSIONS_PER_USER} or {@code FAILED_LOGIN_ATTEMPTS}
     * @param resourceType {@code KERNEL} or {@code PASSWORD}
     * @param limit        a number, {@code UNLIMITED}, or {@code DEFAULT} (the value of the DEFAULT profile)
     */
    public record Limit(String resource, String resourceType, String limit) {
    }
}
