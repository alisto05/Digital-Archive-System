package com.syncpoint.archive.config;

import com.syncpoint.archive.dao.AdminDao;
import com.syncpoint.archive.dto.AdminRegistrationRequest;
import com.syncpoint.archive.util.PasswordPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class DevelopmentAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentAdminConfig.class);

    @Bean
    public ApplicationRunner createInitialAdmin(
            AdminDao adminDao,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.password:}") String password) {

        return args -> {
            if (username.isBlank() || password.isBlank()) {
                log.warn("ADMIN_USERNAME / ADMIN_PASSWORD are not set; skipping initial admin creation.");
                return;
            }

            if ("admin".equalsIgnoreCase(username)) {
                log.warn("ADMIN_USERNAME is 'admin', which matches the seed row in the SQL. "
                        + "Use a different name (e.g. syncadmin) and remove the seeded account.");
            }

            try {
                if (adminDao.usernameExists(username)) {
                    log.info("Initial admin '{}' already exists; skipping creation.", username);
                    return;
                }

                try {
                    PasswordPolicy.validate(password);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("ADMIN_PASSWORD is not usable: " + e.getMessage());
                }

                long adminId = adminDao.registerAdmin(
                        new AdminRegistrationRequest(username, password, "System", "Administrator"),
                        passwordEncoder.encode(password));
                log.info("Created initial admin '{}' (admin id {}).", username, adminId);

            } catch (DataAccessException e) {
              
                log.warn("Could not create the initial admin '{}': {}", username, e.getMostSpecificCause().getMessage());
            }
        };
    }
}
