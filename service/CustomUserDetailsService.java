package com.syncpoint.archive.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public CustomUserDetailsService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT username, password_hash, role FROM users WHERE username = ?", username);

        if (rows.isEmpty()) {
            throw new UsernameNotFoundException("No user with username: " + username);
        }

        Map<String, Object> row = rows.get(0);
        String role = (String) row.get("role"); // PATIENT, STAFF, or ADMIN

        return User.builder()
                .username((String) row.get("username"))
                .password((String) row.get("password_hash"))
                .authorities("ROLE_" + role)
                .build();
    }
}
