package com.syncpoint.archive.dao;

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

/** Patient profile change requests: sp_submit_change_request / sp_resolve_change_request. */
@Repository
public class ProfileChangeDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall submitCall;
    private final SimpleJdbcCall resolveCall;

    public ProfileChangeDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        this.submitCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_submit_change_request")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_patient_id", Types.BIGINT),
                        new SqlParameter("p_requested_by_user_id", Types.BIGINT),
                        new SqlParameter("p_field_name", Types.VARCHAR),
                        new SqlParameter("p_old_value", Types.LONGVARCHAR),
                        new SqlParameter("p_requested_value", Types.LONGVARCHAR),
                        new SqlParameter("p_reason", Types.LONGVARCHAR),
                        new SqlOutParameter("out_change_request_id", Types.BIGINT)
                );

        this.resolveCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_resolve_change_request")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_change_request_id", Types.BIGINT),
                        new SqlParameter("p_approve", Types.BOOLEAN),
                        new SqlParameter("p_reviewed_by_user_id", Types.BIGINT)
                );
    }

    public long submit(long patientId, long requestedByUserId, String fieldName,
                       String oldValue, String requestedValue, String reason) {
        Map<String, Object> out = submitCall.execute(new MapSqlParameterSource()
                .addValue("p_patient_id", patientId)
                .addValue("p_requested_by_user_id", requestedByUserId)
                .addValue("p_field_name", fieldName)
                .addValue("p_old_value", oldValue)
                .addValue("p_requested_value", requestedValue)
                .addValue("p_reason", reason));
        return ((Number) out.get("out_change_request_id")).longValue();
    }

    public List<Map<String, Object>> getForPatient(long patientId) {
        return jdbcTemplate.queryForList("""
                SELECT change_request_id, field_name, old_value, requested_value, reason,
                       status, requested_at, reviewed_at
                FROM profile_change_requests
                WHERE patient_id = ?
                ORDER BY requested_at DESC
                """, patientId);
    }

    public List<Map<String, Object>> getPending() {
        return jdbcTemplate.queryForList("""
                SELECT r.change_request_id, r.patient_id, p.first_name, p.last_name,
                       r.field_name, r.old_value, r.requested_value, r.reason, r.requested_at
                FROM profile_change_requests r
                JOIN patients p ON p.patient_id = r.patient_id
                WHERE r.status = 'PENDING'
                ORDER BY r.requested_at ASC
                """);
    }

    /**
     * Approves or rejects a request through sp_resolve_change_request, only while it is PENDING
     * (the procedure itself would happily re-apply an already-resolved request).
     * The procedure opens its own transaction; the outer @Transactional makes Spring roll
     * back if it fails part-way.
     */
    @Transactional
    public ReviewResult resolveIfPending(long changeRequestId, boolean approve, long reviewedByUserId) {
        List<String> status = jdbcTemplate.queryForList(
                "SELECT status FROM profile_change_requests WHERE change_request_id = ? FOR UPDATE",
                String.class, changeRequestId);
        if (status.isEmpty()) {
            return ReviewResult.NOT_FOUND;
        }
        if (!"PENDING".equals(status.get(0))) {
            return ReviewResult.NOT_PENDING;
        }

        resolveCall.execute(new MapSqlParameterSource()
                .addValue("p_change_request_id", changeRequestId)
                .addValue("p_approve", approve)
                .addValue("p_reviewed_by_user_id", reviewedByUserId));
        return ReviewResult.REVIEWED;
    }
}
