package com.syncpoint.archive.dao;

import com.syncpoint.archive.dto.DocumentRequestDto;
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
public class DocumentRequestDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall requestDocumentCall;

    public DocumentRequestDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.requestDocumentCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_request_document")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_patient_id", Types.BIGINT),
                        new SqlParameter("p_requested_by_user_id", Types.BIGINT),
                        new SqlParameter("p_document_type_id", Types.INTEGER),
                        new SqlParameter("p_request_reason", Types.LONGVARCHAR),
                        new SqlOutParameter("out_request_id", Types.BIGINT)
                );
    }

    public long requestDocument(DocumentRequestDto r) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_patient_id", r.patientId())
                .addValue("p_requested_by_user_id", r.requestedByUserId())
                .addValue("p_document_type_id", r.documentTypeId())
                .addValue("p_request_reason", r.requestReason());
        Map<String, Object> out = requestDocumentCall.execute(params);
        return ((Number) out.get("out_request_id")).longValue();
    }

    /** Powers the "Requested Documents" table on the patient Dashboard. */
    public List<Map<String, Object>> getDocumentRequestsForPatient(long patientId) {
        return jdbcTemplate.queryForList("""
                SELECT dr.request_id, dt.type_name, dr.request_reason, dr.status, dr.requested_at
                FROM document_requests dr
                JOIN document_types dt ON dt.document_type_id = dr.document_type_id
                WHERE dr.patient_id = ?
                ORDER BY dr.requested_at DESC
                """, patientId);
    }
}
