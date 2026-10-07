package com.syncpoint.archive.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class ApprovalDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall reviewPatientCall;

    public ApprovalDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.reviewPatientCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_review_patient")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_patient_id", Types.BIGINT),
                        new SqlParameter("p_new_status", Types.VARCHAR),
                        new SqlParameter("p_reviewed_by_staff_id", Types.BIGINT)
                );
    }

    public List<Map<String, Object>> getPendingPatients() {
        return jdbcTemplate.queryForList("""
                SELECT patient_id, first_name, last_name, id_number, created_at
                FROM patients
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                """);
    }

    /**
     * Approves or rejects a patient through sp_review_patient (the trigger writes the audit row),
     * but only while the patient is PENDING. The row is locked so two staff cannot both decide.
     * reviewedByStaffId is a staff.staff_id, taken from the session.
     */
    @Transactional
    public ReviewResult reviewPatient(long patientId, String newStatus, long reviewedByStaffId) {
        List<String> status = jdbcTemplate.queryForList(
                "SELECT status FROM patients WHERE patient_id = ? FOR UPDATE", String.class, patientId);
        if (status.isEmpty()) {
            return ReviewResult.NOT_FOUND;
        }
        if (!"PENDING".equals(status.get(0))) {
            return ReviewResult.NOT_PENDING;
        }

        reviewPatientCall.execute(new MapSqlParameterSource()
                .addValue("p_patient_id", patientId)
                .addValue("p_new_status", newStatus)
                .addValue("p_reviewed_by_staff_id", reviewedByStaffId));
        return ReviewResult.REVIEWED;
    }

    /** Mirrors db.get_recent_activity_for_patient, including the same
     * "Dr. Lastname" vs "JobTitle CourtesyTitle Lastname" display-name logic. */
    public List<Map<String, Object>> getRecentActivityForPatient(long patientId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.status, p.reviewed_at, s.job_title, s.courtesy_title, s.last_name
                FROM patients p
                LEFT JOIN staff s ON s.staff_id = p.reviewed_by_staff_id
                WHERE p.patient_id = ?
                """, patientId);

        if (rows.isEmpty() || rows.get(0).get("reviewed_at") == null) {
            return List.of();
        }

        Map<String, Object> row = rows.get(0);
        String status = (String) row.get("status");
        String action = "APPROVED".equals(status) ? "Registration approved" : "Registration rejected";
        String displayName = formatStaffDisplayName(
                (String) row.get("job_title"), (String) row.get("courtesy_title"), (String) row.get("last_name"));

        return List.of(Map.of(
                "Action", action,
                "By", displayName,
                "Date", row.get("reviewed_at").toString()
        ));
    }

    private String formatStaffDisplayName(String jobTitle, String courtesyTitle, String lastName) {
        if ("Doctor".equals(jobTitle)) {
            return "Dr. " + lastName;
        }
        String combined = jobTitle + " " + (courtesyTitle != null ? courtesyTitle : "") + " " + lastName;
        return combined.replace("  ", " ").trim();
    }
}
