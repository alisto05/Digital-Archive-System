package com.syncpoint.archive.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Map;

@Repository
public class SearchAndStatsDao {

    private final JdbcTemplate jdbcTemplate;

    public SearchAndStatsDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** Calls sp_record_search — fires the DOCUMENT_SEARCH audit trigger. */
    public void recordSearch(long userId, String searchTerm, String searchScope, int resultsCount) {
        // search_history.search_term is VARCHAR(255)
        String term = searchTerm.length() > 255 ? searchTerm.substring(0, 255) : searchTerm;
        jdbcTemplate.update(
                "CALL sp_record_search(?, ?, ?, ?)",
                userId, term, searchScope, resultsCount);
    }

    /** Replaces the hardcoded st.metric("Total Documents", "24") placeholders. */
    public Map<String, Object> getStaffDashboardStats() {
        return jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(*) FROM documents) AS total_documents,
                    (SELECT COUNT(*) FROM documents WHERE status = 'PENDING') AS pending_approvals,
                    (SELECT COUNT(*) FROM documents WHERE status = 'APPROVED') AS approved_documents,
                    (SELECT COUNT(*) FROM documents WHERE status = 'REJECTED') AS rejected_documents,
                    (SELECT COUNT(*) FROM patients WHERE status = 'PENDING') AS pending_patient_registrations
                """);
    }
}
