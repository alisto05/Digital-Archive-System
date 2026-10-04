package com.syncpoint.archive.dao;

import com.syncpoint.archive.dto.StaffRegistrationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StaffDao {

    private final SimpleJdbcCall registerStaffCall;
    private final JdbcTemplate jdbcTemplate;

    public StaffDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.registerStaffCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_register_staff")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_username", Types.VARCHAR),
                        new SqlParameter("p_password_hash", Types.VARCHAR),
                        new SqlParameter("p_first_name", Types.VARCHAR),
                        new SqlParameter("p_last_name", Types.VARCHAR),
                        new SqlParameter("p_staff_number", Types.VARCHAR),
                        new SqlParameter("p_job_title", Types.VARCHAR),
                        new SqlParameter("p_department", Types.VARCHAR),
                        new SqlParameter("p_email", Types.VARCHAR),
                        new SqlParameter("p_specialization", Types.VARCHAR),
                        new SqlOutParameter("out_staff_id", Types.BIGINT)
                );
    }

    
    @Transactional
    public long registerStaff(StaffRegistrationRequest r, String passwordHash,
                               String staffNumber, String username) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_username", username)
                .addValue("p_password_hash", passwordHash)
                .addValue("p_first_name", r.firstName())
                .addValue("p_last_name", r.lastName())
                .addValue("p_staff_number", staffNumber)
                .addValue("p_job_title", r.jobTitle())
                .addValue("p_department", r.department())
                .addValue("p_email", blankToNull(r.email()))
                .addValue("p_specialization", blankToNull(r.specialization()));

        Map<String, Object> out = registerStaffCall.execute(params);
        long staffId = ((Number) out.get("out_staff_id")).longValue();

        if (r.courtesyTitle() != null && !r.courtesyTitle().isBlank()) {
            jdbcTemplate.update("UPDATE staff SET courtesy_title = ? WHERE staff_id = ?",
                    r.courtesyTitle(), staffId);
        }
        return staffId;
    }

   
    public Optional<Map<String, Object>> getStaffProfile(long staffId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT staff_id, first_name, last_name, staff_number, job_title,
                       courtesy_title, department, email, specialization, created_at
                FROM staff
                WHERE staff_id = ?
                """, staffId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean staffNumberExists(String staffNumber) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff WHERE staff_number = ?", Integer.class, staffNumber);
        return count != null && count > 0;
    }

    public boolean usernameExists(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public boolean emailExists(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

   
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
