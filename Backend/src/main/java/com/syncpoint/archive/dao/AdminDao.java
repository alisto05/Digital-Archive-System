package com.syncpoint.archive.dao;

import com.syncpoint.archive.dto.AdminRegistrationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.Map;

@Repository
public class AdminDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall registerAdminCall;

    public AdminDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.registerAdminCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_register_admin")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_username", Types.VARCHAR),
                        new SqlParameter("p_password_hash", Types.VARCHAR),
                        new SqlParameter("p_first_name", Types.VARCHAR),
                        new SqlParameter("p_last_name", Types.VARCHAR),
                        new SqlOutParameter("out_admin_id", Types.BIGINT)
                );
    }

    @Transactional
    public long registerAdmin(AdminRegistrationRequest r, String passwordHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_username", r.username())
                .addValue("p_password_hash", passwordHash)
                .addValue("p_first_name", r.firstName())
                .addValue("p_last_name", r.lastName());
        Map<String, Object> out = registerAdminCall.execute(params);
        return ((Number) out.get("out_admin_id")).longValue();
    }

    public boolean usernameExists(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    /** Overview counts for an Admin dashboard: patients by status, staff
     * count, and document counts by status (documents/staff tables live
     * in this same database, so plain queries cover it — no new procedure
     * needed for read-only aggregation like this). */
    public Map<String, Object> getOverviewStats() {
        return jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(*) FROM patients) AS total_patients,
                    (SELECT COUNT(*) FROM patients WHERE status = 'PENDING') AS pending_patients,
                    (SELECT COUNT(*) FROM patients WHERE status = 'APPROVED') AS approved_patients,
                    (SELECT COUNT(*) FROM patients WHERE status = 'REJECTED') AS rejected_patients,
                    (SELECT COUNT(*) FROM staff) AS total_staff,
                    (SELECT COUNT(*) FROM users WHERE role = 'ADMIN') AS total_admins,
                    (SELECT COUNT(*) FROM documents) AS total_documents,
                    (SELECT COUNT(*) FROM documents WHERE status = 'PENDING') AS pending_documents
                """);
    }

    public java.util.List<Map<String, Object>> getAllStaff() {
        return jdbcTemplate.queryForList("""
                SELECT staff_id, first_name, last_name, staff_number, job_title,
                       courtesy_title, department, email, specialization, created_at
                FROM staff
                ORDER BY created_at DESC
                """);
    }

    public java.util.List<Map<String, Object>> getAllPatients() {
        return jdbcTemplate.queryForList("""
                SELECT patient_id, first_name, last_name, id_number, status,
                       reviewed_by_staff_id, reviewed_at, created_at
                FROM patients
                ORDER BY created_at DESC
                """);
    }
}
