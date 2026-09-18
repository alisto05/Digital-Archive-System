package com.syncpoint.archive.dao;

import com.syncpoint.archive.dto.DocumentUploadRequest;
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
public class DocumentDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall uploadDocumentCall;
    private final SimpleJdbcCall reviewDocumentCall;

    public DocumentDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        this.uploadDocumentCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_upload_document")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_patient_id", Types.BIGINT),
                        new SqlParameter("p_uploaded_by_user_id", Types.BIGINT),
                        new SqlParameter("p_document_type_id", Types.INTEGER),
                        new SqlParameter("p_original_filename", Types.VARCHAR),
                        new SqlParameter("p_storage_key", Types.VARCHAR),
                        new SqlParameter("p_mime_type", Types.VARCHAR),
                        new SqlParameter("p_file_size_bytes", Types.BIGINT),
                        new SqlParameter("p_checksum_sha256", Types.CHAR),
                        new SqlOutParameter("out_document_id", Types.BIGINT)
                );

        this.reviewDocumentCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_review_document")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_document_id", Types.BIGINT),
                        new SqlParameter("p_new_status", Types.VARCHAR),
                        new SqlParameter("p_reviewed_by_user_id", Types.BIGINT),
                        new SqlParameter("p_rejection_reason", Types.LONGVARCHAR)
                );
    }

    public long uploadDocument(DocumentUploadRequest r) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_patient_id", r.patientId())
                .addValue("p_uploaded_by_user_id", r.uploadedByUserId())
                .addValue("p_document_type_id", r.documentTypeId())
                .addValue("p_original_filename", r.originalFilename())
                .addValue("p_storage_key", r.storageKey())
                .addValue("p_mime_type", r.mimeType())
                .addValue("p_file_size_bytes", r.fileSizeBytes())
                .addValue("p_checksum_sha256", r.checksumSha256());
        Map<String, Object> out = uploadDocumentCall.execute(params);
        return ((Number) out.get("out_document_id")).longValue();
    }

    /** Powers the "My Documents" tab, with optional filename/type search. */
    public List<Map<String, Object>> getDocumentsForPatient(long patientId, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return jdbcTemplate.queryForList("""
                    SELECT d.document_id, dt.type_name, d.original_filename, d.status,
                           d.uploaded_at, d.rejection_reason
                    FROM documents d
                    JOIN document_types dt ON dt.document_type_id = d.document_type_id
                    WHERE d.patient_id = ?
                    ORDER BY d.uploaded_at DESC
                    """, patientId);
        }
        String like = "%" + searchTerm + "%";
        return jdbcTemplate.queryForList("""
                SELECT d.document_id, dt.type_name, d.original_filename, d.status,
                       d.uploaded_at, d.rejection_reason
                FROM documents d
                JOIN document_types dt ON dt.document_type_id = d.document_type_id
                WHERE d.patient_id = ? AND (d.original_filename LIKE ? OR dt.type_name LIKE ?)
                ORDER BY d.uploaded_at DESC
                """, patientId, like, like);
    }

    /** Powers the staff Search page — by patient name, ID number, or document type. */
    public List<Map<String, Object>> searchDocumentsForStaff(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return jdbcTemplate.queryForList("""
                    SELECT d.document_id, p.patient_id, p.first_name, p.last_name, p.id_number,
                           dt.type_name, d.original_filename, d.status, d.uploaded_at
                    FROM documents d
                    JOIN patients p ON p.patient_id = d.patient_id
                    JOIN document_types dt ON dt.document_type_id = d.document_type_id
                    ORDER BY d.uploaded_at DESC
                    """);
        }
        String like = "%" + searchTerm + "%";
        return jdbcTemplate.queryForList("""
                SELECT d.document_id, p.patient_id, p.first_name, p.last_name, p.id_number,
                       dt.type_name, d.original_filename, d.status, d.uploaded_at
                FROM documents d
                JOIN patients p ON p.patient_id = d.patient_id
                JOIN document_types dt ON dt.document_type_id = d.document_type_id
                WHERE p.first_name LIKE ? OR p.last_name LIKE ? OR p.id_number LIKE ? OR dt.type_name LIKE ?
                ORDER BY d.uploaded_at DESC
                """, like, like, like, like);
    }

    public List<Map<String, Object>> getPendingDocumentsForStaff() {
        return jdbcTemplate.queryForList("""
                SELECT d.document_id, p.patient_id, p.first_name, p.last_name,
                       dt.type_name, d.original_filename, d.uploaded_at
                FROM documents d
                JOIN patients p ON p.patient_id = d.patient_id
                JOIN document_types dt ON dt.document_type_id = d.document_type_id
                WHERE d.status = 'PENDING'
                ORDER BY d.uploaded_at ASC
                """);
    }

    public void reviewDocument(long documentId, String newStatus, long reviewedByUserId, String rejectionReason) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_document_id", documentId)
                .addValue("p_new_status", newStatus)
                .addValue("p_reviewed_by_user_id", reviewedByUserId)
                .addValue("p_rejection_reason", rejectionReason);
        reviewDocumentCall.execute(params);
    }

    public List<Map<String, Object>> getDocumentTypes() {
        return jdbcTemplate.queryForList(
                "SELECT document_type_id, type_name FROM document_types ORDER BY type_name");
    }
}
