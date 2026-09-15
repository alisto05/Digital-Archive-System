-- Draft schema reconstructed to match the triggers/procedures file already
-- shared by the team. Confirm column names, sizes, and defaults with
-- whoever wrote the triggers file before treating this as final.

CREATE DATABASE IF NOT EXISTS syncpoint_archive;
USE syncpoint_archive;

-- ============================================================
-- USERS (login accounts for both Patients and Staff)
-- ============================================================
CREATE TABLE users (
    user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('PATIENT', 'STAFF') NOT NULL,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- PATIENTS
-- ============================================================
CREATE TABLE patients (
    patient_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(10),
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    preferred_name VARCHAR(100),
    id_number VARCHAR(20) UNIQUE NOT NULL,
    date_of_birth DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE patient_addresses (
    address_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT UNSIGNED NOT NULL,
    country VARCHAR(100),
    address_line VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    province VARCHAR(100),
    is_current BOOLEAN DEFAULT 1,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE TABLE patient_contacts (
    contact_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT UNSIGNED NOT NULL,
    home_phone VARCHAR(30),
    work_phone VARCHAR(30),
    mobile_phone VARCHAR(30),
    secondary_phone VARCHAR(30),
    email VARCHAR(255),
    secondary_email VARCHAR(255),
    is_current BOOLEAN DEFAULT 1,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE TABLE patient_medical_aid (
    medical_aid_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT UNSIGNED NOT NULL,
    has_medical_aid BOOLEAN DEFAULT 0,
    provider VARCHAR(150),
    membership_number VARCHAR(100),

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

-- ============================================================
-- STAFF
-- ============================================================
CREATE TABLE staff (
    staff_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    staff_number VARCHAR(50) UNIQUE,
    job_title VARCHAR(100),
    department VARCHAR(150),

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ============================================================
-- DOCUMENT TYPES
-- ============================================================
CREATE TABLE document_types (
    document_type_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(100) UNIQUE NOT NULL
);

-- ============================================================
-- DOCUMENTS
-- ============================================================
CREATE TABLE documents (
    document_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT UNSIGNED NOT NULL,
    uploaded_by_user_id BIGINT UNSIGNED NOT NULL,
    document_type_id INT UNSIGNED NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    file_size_bytes BIGINT UNSIGNED,
    checksum_sha256 CHAR(64),
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    approved_by_user_id BIGINT UNSIGNED NULL,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (uploaded_by_user_id) REFERENCES users(user_id),
    FOREIGN KEY (document_type_id) REFERENCES document_types(document_type_id),
    FOREIGN KEY (approved_by_user_id) REFERENCES users(user_id)
);

-- ============================================================
-- DOCUMENT REQUESTS (Staff requesting a document from a Patient)
-- ============================================================
CREATE TABLE document_requests (
    request_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT UNSIGNED NOT NULL,
    requested_by_user_id BIGINT UNSIGNED NOT NULL,
    document_type_id INT UNSIGNED NOT NULL,
    request_reason TEXT,
    status ENUM('PENDING', 'FULFILLED', 'CANCELLED') DEFAULT 'PENDING',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (requested_by_user_id) REFERENCES users(user_id),
    FOREIGN KEY (document_type_id) REFERENCES document_types(document_type_id)
);

-- ============================================================
-- PROFILE CHANGE REQUESTS (Patient requesting an info update)
-- ============================================================
CREATE TABLE profile_change_requests (
    change_request_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT UNSIGNED NOT NULL,
    requested_by_user_id BIGINT UNSIGNED NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    old_value TEXT,
    requested_value TEXT,
    reason TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    reviewed_by_user_id BIGINT UNSIGNED NULL,
    reviewed_at TIMESTAMP NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (requested_by_user_id) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by_user_id) REFERENCES users(user_id)
);

-- ============================================================
-- LOGIN HISTORY
-- ============================================================
CREATE TABLE login_history (
    login_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL,
    username_attempted VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ============================================================
-- SEARCH HISTORY
-- ============================================================
CREATE TABLE search_history (
    search_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    search_term VARCHAR(255),
    search_scope VARCHAR(20),
    results_count INT UNSIGNED,
    searched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ============================================================
-- AUDIT LOGS (auto-populated by the triggers, not inserted directly by the app)
-- ============================================================
CREATE TABLE audit_logs (
    log_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT UNSIGNED NULL,
    description VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ============================================================
-- SEED DATA (adjust/remove once real data exists)
-- ============================================================
INSERT INTO document_types (type_name) VALUES
('Patient Record'),
('Laboratory Report'),
('Radiology Report'),
('Prescription'),
('Consent Form'),
('Administrative Document');

SHOW TABLES;