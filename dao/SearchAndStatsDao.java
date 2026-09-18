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

   
    public void recordSearch(long userId, String searchTerm, String searchScope, int resultsCount) {
        jdbcTemplate.update(
                "CALL sp_record_search(?, ?, ?, ?)",
                userId, searchTerm, searchScope, resultsCount);
    }

    
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
