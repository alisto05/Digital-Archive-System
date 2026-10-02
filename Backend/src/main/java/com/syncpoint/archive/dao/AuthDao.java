package com.syncpoint.archive.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AuthDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall recordLoginCall;
    private final SimpleJdbcCall recordLogoutCall;

    public AuthDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        this.recordLoginCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_record_login_attempt")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_user_id", Types.BIGINT),
                        new SqlParameter("p_username_attempted", Types.VARCHAR),
                        new SqlParameter("p_success", Types.BOOLEAN),
                        new SqlParameter("p_ip_address", Types.VARCHAR),
                        new SqlOutParameter("out_login_id", Types.BIGINT)
                );

        this.recordLogoutCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_record_logout")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_login_id", Types.BIGINT));
    }

    /** Mirrors db.get_patient_login_data exactly. */
    public Map<String, Object> getPatientLoginData(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.user_id, u.username, u.password_hash, p.patient_id, p.status, p.preferred_name
                FROM users u
                JOIN patients p ON p.user_id = u.user_id
                WHERE u.username = ? AND u.role = 'PATIENT'
                """, username);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Mirrors db.get_staff_login_data exactly. */
    public Map<String, Object> getStaffLoginData(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.user_id, u.username, u.password_hash, s.staff_id, s.first_name, s.last_name
                FROM users u
                JOIN staff s ON s.user_id = u.user_id
                WHERE u.username = ? AND u.role = 'STAFF'
                """, username);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Same pattern as getStaffLoginData, against the new admins table. */
    public Map<String, Object> getAdminLoginData(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.user_id, u.username, u.password_hash, a.admin_id, a.first_name, a.last_name
                FROM users u
                JOIN admins a ON a.user_id = u.user_id
                WHERE u.username = ? AND u.role = 'ADMIN'
                """, username);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Current role for a username, or empty if the user no longer exists
     * (used by the session filter to invalidate stale sessions). */
    public Optional<String> findUserRole(String username) {
        List<String> roles = jdbcTemplate.queryForList(
                "SELECT role FROM users WHERE username = ?", String.class, username);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }

    public long recordLoginAttempt(Long userId, String usernameAttempted, boolean success, String ipAddress) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_user_id", userId, Types.BIGINT)
                .addValue("p_username_attempted", usernameAttempted)
                .addValue("p_success", success)
                .addValue("p_ip_address", ipAddress);
        Map<String, Object> out = recordLoginCall.execute(params);
        return ((Number) out.get("out_login_id")).longValue();
    }

    public void recordLogout(long loginId) {
        recordLogoutCall.execute(new MapSqlParameterSource().addValue("p_login_id", loginId));
    }

    /** Used to migrate an account off the legacy salt$sha256 hash the moment
     * it logs in successfully — see AuthController. */
    public void updatePasswordHash(long userId, String newPasswordHash) {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE user_id = ?", newPasswordHash, userId);
    }
}
