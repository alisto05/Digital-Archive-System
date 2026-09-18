DROP DATABASE IF EXISTS syncpoint_archive;
CREATE DATABASE syncpoint_archive;
USE syncpoint_archive;

-- ------------------------------------------------------------
-- users — login accounts for Patients, Staff, AND Admins
-- ------------------------------------------------------------
CREATE TABLE users (
    user_id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('PATIENT', 'STAFF', 'ADMIN') NOT NULL,
    last_login_at   TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- patients — status/reviewed_by_staff_id/reviewed_at are required
-- by db.get_patient_login_data, db.get_pending_patients,
-- db.update_patient_status, and pages/8_Approvals.py.
-- ------------------------------------------------------------
CREATE TABLE patients (
    patient_id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT UNSIGNED NOT NULL,
    title                   VARCHAR(10),
    first_name              VARCHAR(100) NOT NULL,
    middle_name             VARCHAR(100),
    last_name               VARCHAR(100) NOT NULL,
    preferred_name          VARCHAR(100),
    id_number               VARCHAR(20) NOT NULL UNIQUE,
    date_of_birth           DATE NOT NULL,
    status                  ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by_staff_id    BIGINT UNSIGNED NULL,
    reviewed_at              TIMESTAMP NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
    -- fk_patients_reviewed_by added after `staff` exists, below
);

CREATE TABLE patient_addresses (
    address_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id      BIGINT UNSIGNED NOT NULL,
    country         VARCHAR(100),
    address_line    VARCHAR(255),
    city            VARCHAR(100),
    postal_code     VARCHAR(20),
    province        VARCHAR(100),
    is_current      BOOLEAN NOT NULL DEFAULT TRUE,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE TABLE patient_contacts (
    contact_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id       BIGINT UNSIGNED NOT NULL,
    home_phone       VARCHAR(30),
    work_phone       VARCHAR(30),
    mobile_phone     VARCHAR(30),
    secondary_phone  VARCHAR(30),
    email            VARCHAR(255),
    secondary_email  VARCHAR(255),
    is_current       BOOLEAN NOT NULL DEFAULT TRUE,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE TABLE patient_medical_aid (
    medical_aid_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id         BIGINT UNSIGNED NOT NULL UNIQUE,
    has_medical_aid    BOOLEAN NOT NULL DEFAULT FALSE,
    provider           VARCHAR(150),
    membership_number  VARCHAR(100),

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

-- ------------------------------------------------------------
-- staff — email/specialization required by the Admin "Add Staff
-- Member" form.
-- ------------------------------------------------------------
CREATE TABLE staff (
    staff_id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT UNSIGNED NOT NULL,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    staff_number      VARCHAR(50) UNIQUE,
    job_title         VARCHAR(100),
    courtesy_title    VARCHAR(10) NULL,
    department        VARCHAR(150),
    email             VARCHAR(255) UNIQUE,
    specialization    VARCHAR(150),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_reviewed_by
    FOREIGN KEY (reviewed_by_staff_id) REFERENCES staff(staff_id);

-- ------------------------------------------------------------
-- admins — a separate identity table, same pattern as `staff`.
-- ------------------------------------------------------------
CREATE TABLE admins (
    admin_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ------------------------------------------------------------
-- Seed a single bootstrap Admin account so there's a way to log
-- in for the first time. Username: admin  Password: ChangeMe123!
-- >>> CHANGE THIS PASSWORD IMMEDIATELY AFTER FIRST LOGIN. <<<
-- ------------------------------------------------------------
INSERT INTO users (username, password_hash, role)
VALUES ('admin',
        'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4$2639f6cc7ef8fdcf26342445a1abccc8275520b8c3d55646413269954c4c5d56',
        'ADMIN');

INSERT INTO admins (user_id, first_name, last_name)
SELECT user_id, 'System', 'Administrator' FROM users WHERE username = 'admin';

-- ------------------------------------------------------------
-- document_types — lookup for the upload/search pages
-- ------------------------------------------------------------
CREATE TABLE document_types (
    document_type_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type_name          VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO document_types (type_name) VALUES
    ('Patient Record'),
    ('Laboratory Report'),
    ('Radiology Report'),
    ('Prescription'),
    ('Consent Form'),
    ('Administrative Document');

-- ------------------------------------------------------------
-- documents
-- ------------------------------------------------------------
CREATE TABLE documents (
    document_id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id              BIGINT UNSIGNED NOT NULL,
    uploaded_by_user_id      BIGINT UNSIGNED NOT NULL,
    document_type_id         INT UNSIGNED NOT NULL,
    original_filename         VARCHAR(255) NOT NULL,
    storage_key               VARCHAR(500) NOT NULL,
    mime_type                 VARCHAR(100),
    file_size_bytes           BIGINT UNSIGNED,
    checksum_sha256           CHAR(64),
    status                    ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    approved_by_user_id       BIGINT UNSIGNED NULL,
    approved_at                TIMESTAMP NULL,
    rejection_reason           TEXT,
    uploaded_at                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (uploaded_by_user_id) REFERENCES users(user_id),
    FOREIGN KEY (document_type_id) REFERENCES document_types(document_type_id),
    FOREIGN KEY (approved_by_user_id) REFERENCES users(user_id),
    INDEX idx_documents_patient (patient_id),
    INDEX idx_documents_status (status)
);

-- ------------------------------------------------------------
-- document_requests — Staff requesting a document from a Patient
-- ------------------------------------------------------------
CREATE TABLE document_requests (
    request_id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id               BIGINT UNSIGNED NOT NULL,
    requested_by_user_id      BIGINT UNSIGNED NOT NULL,
    document_type_id          INT UNSIGNED NOT NULL,
    request_reason            TEXT,
    status                     ENUM('PENDING', 'FULFILLED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    requested_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (requested_by_user_id) REFERENCES users(user_id),
    FOREIGN KEY (document_type_id) REFERENCES document_types(document_type_id)
);

-- ------------------------------------------------------------
-- profile_change_requests — Patient requesting an info update
-- ------------------------------------------------------------
CREATE TABLE profile_change_requests (
    change_request_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id                BIGINT UNSIGNED NOT NULL,
    requested_by_user_id       BIGINT UNSIGNED NOT NULL,
    field_name                  VARCHAR(100) NOT NULL,
    old_value                    TEXT,
    requested_value              TEXT,
    reason                        TEXT,
    status                        ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id            BIGINT UNSIGNED NULL,
    reviewed_at                     TIMESTAMP NULL,
    requested_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (requested_by_user_id) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by_user_id) REFERENCES users(user_id)
);

-- ------------------------------------------------------------
-- login_history
-- ------------------------------------------------------------
CREATE TABLE login_history (
    login_id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT UNSIGNED NULL,
    username_attempted     VARCHAR(50) NOT NULL,
    success                BOOLEAN NOT NULL,
    ip_address             VARCHAR(45),
    login_time             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time            TIMESTAMP NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ------------------------------------------------------------
-- search_history
-- ------------------------------------------------------------
CREATE TABLE search_history (
    search_id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT UNSIGNED NOT NULL,
    search_term        VARCHAR(255),
    search_scope        VARCHAR(20),
    results_count        INT UNSIGNED,
    searched_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ------------------------------------------------------------
-- audit_logs — populated by triggers only, never inserted directly
-- ------------------------------------------------------------
CREATE TABLE audit_logs (
    log_id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT UNSIGNED NULL,
    action_type         VARCHAR(50) NOT NULL,
    entity_type          VARCHAR(50) NOT NULL,
    entity_id             BIGINT UNSIGNED NULL,
    description            VARCHAR(500),
    ip_address              VARCHAR(45),
    created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_audit_logs_entity (entity_type, entity_id),
    INDEX idx_audit_logs_user (user_id),
    INDEX idx_audit_logs_action (action_type),
    INDEX idx_audit_logs_created (created_at)
);


-- ============================================================
-- PART 2 — TRIGGERS AND STORED PROCEDURES
-- (runs against the schema just created above)
-- ============================================================

DELIMITER $$

CREATE TRIGGER trg_patients_after_insert
AFTER INSERT ON patients
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
    VALUES (NEW.user_id, 'REGISTER', 'patients', NEW.patient_id,
            CONCAT('Patient account registered: ', NEW.first_name, ' ', NEW.last_name));
END$$

-- Fills a gap from the earlier version: nothing previously moved a
-- patient from PENDING to APPROVED/REJECTED. reviewed_by_staff_id
-- points at staff.staff_id, not users.user_id, so this looks up the
-- reviewer's actual user_id before logging (audit_logs.user_id is a
-- real FK to users, and staff_id/user_id are not interchangeable).
CREATE TRIGGER trg_patients_after_update
AFTER UPDATE ON patients
FOR EACH ROW
BEGIN
    DECLARE v_reviewer_user_id BIGINT UNSIGNED;
    IF NEW.status <> OLD.status AND NEW.status IN ('APPROVED', 'REJECTED') THEN
        SELECT user_id INTO v_reviewer_user_id
            FROM staff WHERE staff_id = NEW.reviewed_by_staff_id;
        INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
        VALUES (v_reviewer_user_id, CONCAT('PATIENT_', NEW.status), 'patients', NEW.patient_id,
                CONCAT('Patient account ', LOWER(NEW.status), ': ',
                       NEW.first_name, ' ', NEW.last_name));
    END IF;
END$$

CREATE TRIGGER trg_documents_after_insert
AFTER INSERT ON documents
FOR EACH ROW
BEGIN
    DECLARE v_type_name VARCHAR(100);
    SELECT type_name INTO v_type_name
        FROM document_types WHERE document_type_id = NEW.document_type_id;
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
    VALUES (NEW.uploaded_by_user_id, 'DOCUMENT_UPLOAD', 'documents', NEW.document_id,
            CONCAT(v_type_name, ' uploaded for patient #', NEW.patient_id));
END$$

CREATE TRIGGER trg_documents_after_update
AFTER UPDATE ON documents
FOR EACH ROW
BEGIN
    DECLARE v_type_name VARCHAR(100);
    IF NEW.status <> OLD.status AND NEW.status = 'APPROVED' THEN
        SELECT type_name INTO v_type_name
            FROM document_types WHERE document_type_id = NEW.document_type_id;
        INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
        VALUES (NEW.approved_by_user_id, 'DOCUMENT_APPROVE', 'documents', NEW.document_id,
                CONCAT(v_type_name, ' approved for patient #', NEW.patient_id));
    ELSEIF NEW.status <> OLD.status AND NEW.status = 'REJECTED' THEN
        SELECT type_name INTO v_type_name
            FROM document_types WHERE document_type_id = NEW.document_type_id;
        INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
        VALUES (NEW.approved_by_user_id, 'DOCUMENT_REJECT', 'documents', NEW.document_id,
                CONCAT(v_type_name, ' rejected for patient #', NEW.patient_id,
                       COALESCE(CONCAT(' - reason: ', NEW.rejection_reason), '')));
    END IF;
END$$

CREATE TRIGGER trg_document_requests_after_insert
AFTER INSERT ON document_requests
FOR EACH ROW
BEGIN
    DECLARE v_type_name VARCHAR(100);
    SELECT type_name INTO v_type_name
        FROM document_types WHERE document_type_id = NEW.document_type_id;
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
    VALUES (NEW.requested_by_user_id, 'DOCUMENT_REQUEST', 'document_requests', NEW.request_id,
            CONCAT(v_type_name, ' requested from patient #', NEW.patient_id));
END$$

CREATE TRIGGER trg_profile_change_after_insert
AFTER INSERT ON profile_change_requests
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
    VALUES (NEW.requested_by_user_id, 'PROFILE_CHANGE_REQUEST', 'profile_change_requests',
            NEW.change_request_id,
            CONCAT('Requested change to ', NEW.field_name, ' for patient #', NEW.patient_id));
END$$

CREATE TRIGGER trg_profile_change_after_update
AFTER UPDATE ON profile_change_requests
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status AND NEW.status IN ('APPROVED', 'REJECTED') THEN
        INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
        VALUES (NEW.reviewed_by_user_id, 'PROFILE_CHANGE_REQUEST', 'profile_change_requests',
                NEW.change_request_id,
                CONCAT(NEW.field_name, ' change ', LOWER(NEW.status), ' for patient #', NEW.patient_id));
    END IF;
END$$

-- Fills the same registration-audit gap patients already had covered:
-- creating a Staff or Admin account previously logged nothing.
CREATE TRIGGER trg_staff_after_insert
AFTER INSERT ON staff
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
    VALUES (NEW.user_id, 'REGISTER', 'staff', NEW.staff_id,
            CONCAT('Staff account created: ', NEW.first_name, ' ', NEW.last_name,
                   COALESCE(CONCAT(' (', NEW.job_title, ')'), '')));
END$$

CREATE TRIGGER trg_admins_after_insert
AFTER INSERT ON admins
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
    VALUES (NEW.user_id, 'REGISTER', 'admins', NEW.admin_id,
            CONCAT('Admin account created: ', NEW.first_name, ' ', NEW.last_name));
END$$

CREATE TRIGGER trg_login_history_after_insert
AFTER INSERT ON login_history
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description, ip_address)
    VALUES (NEW.user_id, IF(NEW.success, 'LOGIN_SUCCESS', 'LOGIN_FAILED'), 'login_history',
            NEW.login_id, CONCAT('Login attempt for username: ', NEW.username_attempted),
            NEW.ip_address);
    IF NEW.success = 1 AND NEW.user_id IS NOT NULL THEN
        UPDATE users SET last_login_at = NEW.login_time WHERE user_id = NEW.user_id;
    END IF;
END$$

CREATE TRIGGER trg_login_history_after_update
AFTER UPDATE ON login_history
FOR EACH ROW
BEGIN
    IF NEW.logout_time IS NOT NULL AND OLD.logout_time IS NULL THEN
        INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description)
        VALUES (NEW.user_id, 'LOGOUT', 'login_history', NEW.login_id,
                CONCAT('User logged out: ', NEW.username_attempted));
    END IF;
END$$

CREATE TRIGGER trg_search_history_after_insert
AFTER INSERT ON search_history
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action_type, entity_type, description)
    VALUES (NEW.user_id, 'DOCUMENT_SEARCH', 'search_history',
            CONCAT('Searched "', NEW.search_term, '" (', NEW.results_count, ' results)'));
END$$

CREATE PROCEDURE sp_register_patient (
    IN p_username VARCHAR(50), IN p_password_hash VARCHAR(255),
    IN p_title VARCHAR(10), IN p_first_name VARCHAR(100), IN p_middle_name VARCHAR(100),
    IN p_last_name VARCHAR(100), IN p_preferred_name VARCHAR(100),
    IN p_id_number VARCHAR(20), IN p_date_of_birth DATE,
    IN p_country VARCHAR(100), IN p_address_line VARCHAR(255), IN p_city VARCHAR(100),
    IN p_postal_code VARCHAR(20), IN p_province VARCHAR(100),
    IN p_home_phone VARCHAR(30), IN p_work_phone VARCHAR(30),
    IN p_mobile_phone VARCHAR(30), IN p_secondary_phone VARCHAR(30),
    IN p_email VARCHAR(255), IN p_secondary_email VARCHAR(255),
    IN p_has_medical_aid BOOLEAN, IN p_provider VARCHAR(150), IN p_membership_number VARCHAR(100),
    OUT out_patient_id BIGINT UNSIGNED
)
BEGIN
    DECLARE new_user_id BIGINT UNSIGNED;
    DECLARE new_patient_id BIGINT UNSIGNED;
    START TRANSACTION;
    INSERT INTO users (username, password_hash, role)
    VALUES (p_username, p_password_hash, 'PATIENT');
    SET new_user_id = LAST_INSERT_ID();
    INSERT INTO patients (user_id, title, first_name, middle_name, last_name,
                           preferred_name, id_number, date_of_birth)
    VALUES (new_user_id, p_title, p_first_name, p_middle_name, p_last_name,
            p_preferred_name, p_id_number, p_date_of_birth);
    SET new_patient_id = LAST_INSERT_ID();
    INSERT INTO patient_addresses (patient_id, country, address_line, city, postal_code, province)
    VALUES (new_patient_id, p_country, p_address_line, p_city, p_postal_code, p_province);
    INSERT INTO patient_contacts (patient_id, home_phone, work_phone, mobile_phone,
                                   secondary_phone, email, secondary_email)
    VALUES (new_patient_id, p_home_phone, p_work_phone, p_mobile_phone,
            p_secondary_phone, p_email, p_secondary_email);
    INSERT INTO patient_medical_aid (patient_id, has_medical_aid, provider, membership_number)
    VALUES (new_patient_id, p_has_medical_aid, p_provider, p_membership_number);
    SET out_patient_id = new_patient_id;
    COMMIT;
END$$

CREATE PROCEDURE sp_register_staff (
    IN p_username VARCHAR(50), IN p_password_hash VARCHAR(255),
    IN p_first_name VARCHAR(100), IN p_last_name VARCHAR(100),
    IN p_staff_number VARCHAR(50), IN p_job_title VARCHAR(100), IN p_department VARCHAR(150),
    IN p_email VARCHAR(255), IN p_specialization VARCHAR(150),
    OUT out_staff_id BIGINT UNSIGNED
)
BEGIN
    DECLARE new_user_id BIGINT UNSIGNED;
    START TRANSACTION;
    INSERT INTO users (username, password_hash, role)
    VALUES (p_username, p_password_hash, 'STAFF');
    SET new_user_id = LAST_INSERT_ID();
    INSERT INTO staff (user_id, first_name, last_name, staff_number, job_title,
                        department, email, specialization)
    VALUES (new_user_id, p_first_name, p_last_name, p_staff_number, p_job_title,
            p_department, p_email, p_specialization);
    SET out_staff_id = LAST_INSERT_ID();
    COMMIT;
END$$

-- Approves or rejects a PENDING patient account. p_reviewed_by_staff_id
-- is a staff.staff_id (not a users.user_id) — trg_patients_after_update
-- resolves it to the right user_id for the audit log entry.
CREATE PROCEDURE sp_review_patient (
    IN p_patient_id BIGINT UNSIGNED, IN p_new_status VARCHAR(20),
    IN p_reviewed_by_staff_id BIGINT UNSIGNED
)
BEGIN
    UPDATE patients
    SET status = p_new_status,
        reviewed_by_staff_id = p_reviewed_by_staff_id,
        reviewed_at = NOW()
    WHERE patient_id = p_patient_id;
END$$

CREATE PROCEDURE sp_register_admin (
    IN p_username VARCHAR(50), IN p_password_hash VARCHAR(255),
    IN p_first_name VARCHAR(100), IN p_last_name VARCHAR(100),
    OUT out_admin_id BIGINT UNSIGNED
)
BEGIN
    DECLARE new_user_id BIGINT UNSIGNED;
    START TRANSACTION;
    INSERT INTO users (username, password_hash, role)
    VALUES (p_username, p_password_hash, 'ADMIN');
    SET new_user_id = LAST_INSERT_ID();
    INSERT INTO admins (user_id, first_name, last_name)
    VALUES (new_user_id, p_first_name, p_last_name);
    SET out_admin_id = LAST_INSERT_ID();
    COMMIT;
END$$

CREATE PROCEDURE sp_record_login_attempt (
    IN p_user_id BIGINT UNSIGNED, IN p_username_attempted VARCHAR(50),
    IN p_success BOOLEAN, IN p_ip_address VARCHAR(45),
    OUT out_login_id BIGINT UNSIGNED
)
BEGIN
    INSERT INTO login_history (user_id, username_attempted, success, ip_address)
    VALUES (p_user_id, p_username_attempted, p_success, p_ip_address);
    SET out_login_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_record_logout (
    IN p_login_id BIGINT UNSIGNED
)
BEGIN
    UPDATE login_history SET logout_time = NOW() WHERE login_id = p_login_id;
END$$

CREATE PROCEDURE sp_record_search (
    IN p_user_id BIGINT UNSIGNED, IN p_search_term VARCHAR(255),
    IN p_search_scope VARCHAR(20), IN p_results_count INT UNSIGNED
)
BEGIN
    INSERT INTO search_history (user_id, search_term, search_scope, results_count)
    VALUES (p_user_id, p_search_term, p_search_scope, p_results_count);
END$$

CREATE PROCEDURE sp_upload_document (
    IN p_patient_id BIGINT UNSIGNED, IN p_uploaded_by_user_id BIGINT UNSIGNED,
    IN p_document_type_id INT UNSIGNED, IN p_original_filename VARCHAR(255),
    IN p_storage_key VARCHAR(500), IN p_mime_type VARCHAR(100),
    IN p_file_size_bytes BIGINT UNSIGNED, IN p_checksum_sha256 CHAR(64),
    OUT out_document_id BIGINT UNSIGNED
)
BEGIN
    INSERT INTO documents (patient_id, uploaded_by_user_id, document_type_id,
                            original_filename, storage_key, mime_type,
                            file_size_bytes, checksum_sha256)
    VALUES (p_patient_id, p_uploaded_by_user_id, p_document_type_id,
            p_original_filename, p_storage_key, p_mime_type,
            p_file_size_bytes, p_checksum_sha256);
    SET out_document_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_review_document (
    IN p_document_id BIGINT UNSIGNED, IN p_new_status VARCHAR(20),
    IN p_reviewed_by_user_id BIGINT UNSIGNED, IN p_rejection_reason TEXT
)
BEGIN
    UPDATE documents
    SET status = p_new_status,
        approved_by_user_id = p_reviewed_by_user_id,
        approved_at = NOW(),
        rejection_reason = p_rejection_reason
    WHERE document_id = p_document_id;
END$$

CREATE PROCEDURE sp_request_document (
    IN p_patient_id BIGINT UNSIGNED, IN p_requested_by_user_id BIGINT UNSIGNED,
    IN p_document_type_id INT UNSIGNED, IN p_request_reason TEXT,
    OUT out_request_id BIGINT UNSIGNED
)
BEGIN
    INSERT INTO document_requests (patient_id, requested_by_user_id, document_type_id, request_reason)
    VALUES (p_patient_id, p_requested_by_user_id, p_document_type_id, p_request_reason);
    SET out_request_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_submit_change_request (
    IN p_patient_id BIGINT UNSIGNED, IN p_requested_by_user_id BIGINT UNSIGNED,
    IN p_field_name VARCHAR(100), IN p_old_value TEXT, IN p_requested_value TEXT,
    IN p_reason TEXT,
    OUT out_change_request_id BIGINT UNSIGNED
)
BEGIN
    INSERT INTO profile_change_requests (patient_id, requested_by_user_id, field_name,
                                          old_value, requested_value, reason)
    VALUES (p_patient_id, p_requested_by_user_id, p_field_name, p_old_value, p_requested_value, p_reason);
    SET out_change_request_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_resolve_change_request (
    IN p_change_request_id BIGINT UNSIGNED, IN p_approve BOOLEAN, IN p_reviewed_by_user_id BIGINT UNSIGNED
)
BEGIN
    DECLARE v_patient_id BIGINT UNSIGNED;
    DECLARE v_field VARCHAR(100);
    DECLARE v_value TEXT;
    SELECT patient_id, field_name, requested_value
        INTO v_patient_id, v_field, v_value
        FROM profile_change_requests WHERE change_request_id = p_change_request_id;
    START TRANSACTION;
    IF p_approve = 1 THEN
        IF v_field = 'address_line' THEN
            UPDATE patient_addresses SET address_line = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'city' THEN
            UPDATE patient_addresses SET city = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'postal_code' THEN
            UPDATE patient_addresses SET postal_code = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'province' THEN
            UPDATE patient_addresses SET province = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'home_phone' THEN
            UPDATE patient_contacts SET home_phone = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'work_phone' THEN
            UPDATE patient_contacts SET work_phone = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'mobile_phone' THEN
            UPDATE patient_contacts SET mobile_phone = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'secondary_phone' THEN
            UPDATE patient_contacts SET secondary_phone = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'email' THEN
            UPDATE patient_contacts SET email = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'secondary_email' THEN
            UPDATE patient_contacts SET secondary_email = v_value WHERE patient_id = v_patient_id AND is_current = 1;
        ELSEIF v_field = 'provider' THEN
            UPDATE patient_medical_aid SET provider = v_value WHERE patient_id = v_patient_id;
        ELSEIF v_field = 'membership_number' THEN
            UPDATE patient_medical_aid SET membership_number = v_value WHERE patient_id = v_patient_id;
        END IF;
        UPDATE profile_change_requests
        SET status = 'APPROVED', reviewed_by_user_id = p_reviewed_by_user_id, reviewed_at = NOW()
        WHERE change_request_id = p_change_request_id;
    ELSE
        UPDATE profile_change_requests
        SET status = 'REJECTED', reviewed_by_user_id = p_reviewed_by_user_id, reviewed_at = NOW()
        WHERE change_request_id = p_change_request_id;
    END IF;
    COMMIT;
END$$

DELIMITER ;
