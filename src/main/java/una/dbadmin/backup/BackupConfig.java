package una.dbadmin.backup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackupConfig {

    private static final String THIN_PREFIX = "jdbc:oracle:thin:@";

    @Bean
    ProcessRunner processRunner() {
        return new SystemProcessRunner();
    }

    @Bean
    ExpdpCommandBuilder expdpCommandBuilder(@Value("${spring.datasource.url}") String url,
                                            @Value("${spring.datasource.username}") String user,
                                            @Value("${spring.datasource.password}") String password) {
        return new ExpdpCommandBuilder(user, password, connectIdentifier(url));
    }

    @Bean
    BackupService backupService(ExpdpCommandBuilder builder, ProcessRunner runner,
                                @Value("${spring.datasource.password}") String password) {
        return new BackupService(builder, runner, password);
    }

    /** Converts {@code jdbc:oracle:thin:@//host:port/service} into the {@code //host:port/service} form expdp expects. */
    static String connectIdentifier(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(THIN_PREFIX)) {
            throw new IllegalArgumentException("Unsupported database URL; expected " + THIN_PREFIX + "//host:port/service");
        }
        return jdbcUrl.substring(THIN_PREFIX.length());
    }
}
