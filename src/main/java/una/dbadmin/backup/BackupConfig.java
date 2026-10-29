package una.dbadmin.backup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Bean
    ImpdpCommandBuilder impdpCommandBuilder(@Value("${spring.datasource.url}") String url,
                                            @Value("${spring.datasource.username}") String user,
                                            @Value("${spring.datasource.password}") String password) {
        return new ImpdpCommandBuilder(user, password, connectIdentifier(url));
    }

    @Bean
    OracleCatalog oracleCatalog(JdbcTemplate jdbcTemplate) {
        return new JdbcOracleCatalog(jdbcTemplate);
    }

    @Bean
    RecoveryService recoveryService(ImpdpCommandBuilder builder, ProcessRunner runner, OracleCatalog catalog,
                                    @Value("${spring.datasource.password}") String password) {
        return new RecoveryService(builder, runner, catalog, password);
    }

    /** Converts {@code jdbc:oracle:thin:@//host:port/service} into the {@code //host:port/service} form expdp expects. */
    static String connectIdentifier(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(THIN_PREFIX)) {
            throw new IllegalArgumentException("URL de base de datos no soportada; se esperaba " + THIN_PREFIX + "//host:port/service");
        }
        return jdbcUrl.substring(THIN_PREFIX.length());
    }
}
