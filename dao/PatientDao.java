package com.syncpoint.archive.dao;

import com.syncpoint.archive.dto.PatientRegistrationRequest;
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
public class PatientDao {

    private final SimpleJdbcCall registerPatientCall;
    private final JdbcTemplate jdbcTemplate;

    public PatientDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.registerPatientCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_register_patient")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_username", Types.VARCHAR),
                        new SqlParameter("p_password_hash", Types.VARCHAR),
                        new SqlParameter("p_title", Types.VARCHAR),
                        new SqlParameter("p_first_name", Types.VARCHAR),
                        new SqlParameter("p_middle_name", Types.VARCHAR),
                        new SqlParameter("p_last_name", Types.VARCHAR),
                        new SqlParameter("p_preferred_name", Types.VARCHAR),
                        new SqlParameter("p_id_number", Types.VARCHAR),
                        new SqlParameter("p_date_of_birth", Types.DATE),
                        new SqlParameter("p_country", Types.VARCHAR),
                        new SqlParameter("p_address_line", Types.VARCHAR),
                        new SqlParameter("p_city", Types.VARCHAR),
                        new SqlParameter("p_postal_code", Types.VARCHAR),
                        new SqlParameter("p_province", Types.VARCHAR),
                        new SqlParameter("p_home_phone", Types.VARCHAR),
                        new SqlParameter("p_work_phone", Types.VARCHAR),
                        new SqlParameter("p_mobile_phone", Types.VARCHAR),
                        new SqlParameter("p_secondary_phone", Types.VARCHAR),
                        new SqlParameter("p_email", Types.VARCHAR),
                        new SqlParameter("p_secondary_email", Types.VARCHAR),
                        new SqlParameter("p_has_medical_aid", Types.BOOLEAN),
                        new SqlParameter("p_provider", Types.VARCHAR),
                        new SqlParameter("p_membership_number", Types.VARCHAR),
                        new SqlOutParameter("out_patient_id", Types.BIGINT)
                );
    }

    public long registerPatient(PatientRegistrationRequest r, String passwordHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_username", r.username())
                .addValue("p_password_hash", passwordHash)
                .addValue("p_title", r.title())
                .addValue("p_first_name", r.firstName())
                .addValue("p_middle_name", r.middleName())
                .addValue("p_last_name", r.lastName())
                .addValue("p_preferred_name", r.preferredName())
                .addValue("p_id_number", r.idNumber())
                .addValue("p_date_of_birth", java.sql.Date.valueOf(r.dateOfBirth()))
                .addValue("p_country", r.country())
                .addValue("p_address_line", r.addressLine())
                .addValue("p_city", r.city())
                .addValue("p_postal_code", r.postalCode())
                .addValue("p_province", r.province())
                .addValue("p_home_phone", r.homePhone())
                .addValue("p_work_phone", r.workPhone())
                .addValue("p_mobile_phone", r.mobilePhone())
                .addValue("p_secondary_phone", r.secondaryPhone())
                .addValue("p_email", r.email())
                .addValue("p_secondary_email", r.secondaryEmail())
                .addValue("p_has_medical_aid", r.hasMedicalAid())
                .addValue("p_provider", r.provider())
                .addValue("p_membership_number", r.membershipNumber());

        Map<String, Object> out = registerPatientCall.execute(params);
        return ((Number) out.get("out_patient_id")).longValue();
    }

   
    public Map<String, Object> getPatientProfile(long patientId) {
        return jdbcTemplate.queryForMap("""
                SELECT p.title, p.first_name, p.middle_name, p.last_name, p.preferred_name,
                       p.id_number, p.date_of_birth,
                       a.address_line, a.city, a.province, a.postal_code, a.country,
                       c.home_phone, c.work_phone, c.mobile_phone, c.secondary_phone,
                       c.email, c.secondary_email,
                       m.has_medical_aid, m.provider, m.membership_number
                FROM patients p
                LEFT JOIN patient_addresses a ON a.patient_id = p.patient_id AND a.is_current = 1
                LEFT JOIN patient_contacts c ON c.patient_id = p.patient_id AND c.is_current = 1
                LEFT JOIN patient_medical_aid m ON m.patient_id = p.patient_id
                WHERE p.patient_id = ?
                """, patientId);
    }
}
