package com.syncpoint.archive.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Repository
public class ApprovalDao {

    private final JdbcTemplate jdbcTemplate;

    public ApprovalDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Map<String, Object>> getPendingPatients() {
        return jdbcTemplate.queryForList("""
                SELECT patient_id, first_name, last_name, id_number, created_at
                FROM patients
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                """);
    }

    public void updatePatientStatus(long patientId, String newStatus, long reviewedByStaffId) {
        jdbcTemplate.update("""
                UPDATE patients
                SET status = ?, reviewed_by_staff_id = ?, reviewed_at = NOW()
                WHERE patient_id = ?
                """, newStatus, reviewedByStaffId, patientId);
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
