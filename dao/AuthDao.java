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

   
    public Map<String, Object> getPatientLoginData(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.user_id, u.password_hash, p.patient_id, p.status, p.preferred_name
                FROM users u
                JOIN patients p ON p.user_id = u.user_id
                WHERE u.username = ? AND u.role = 'PATIENT'
                """, username);
        return rows.isEmpty() ? null : rows.get(0);
    }

 
    public Map<String, Object> getStaffLoginData(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.user_id, u.password_hash, s.staff_id, s.first_name, s.last_name
                FROM users u
                JOIN staff s ON s.user_id = u.user_id
                WHERE u.username = ? AND u.role = 'STAFF'
                """, username);
        return rows.isEmpty() ? null : rows.get(0);
    }

  
    public Map<String, Object> getAdminLoginData(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.user_id, u.password_hash, a.admin_id, a.first_name, a.last_name
                FROM users u
                JOIN admins a ON a.user_id = u.user_id
                WHERE u.username = ? AND u.role = 'ADMIN'
                """, username);
        return rows.isEmpty() ? null : rows.get(0);
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
}
