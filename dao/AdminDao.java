package com.syncpoint.archive.dao;

import com.syncpoint.archive.dto.AdminRegistrationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

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
}
