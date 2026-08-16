CREATE DATABASE hospital_archive;

USE hospital_archive;


CREATE TABLE Users (
    UserID INT AUTO_INCREMENT PRIMARY KEY,
    FullName VARCHAR(100) NOT NULL,
    Username VARCHAR(50) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL,
    Role ENUM('Admin', 'Staff') NOT NULL,
    Email VARCHAR(100)
);

CREATE TABLE Patients (
    PatientID INT AUTO_INCREMENT PRIMARY KEY,
    HospitalNumber VARCHAR(20) UNIQUE NOT NULL,
    FirstName VARCHAR(50) NOT NULL,
    LastName VARCHAR(50) NOT NULL,
    DateOfBirth DATE NOT NULL,
    Gender ENUM('Male', 'Female', 'Other') NOT NULL,
    Phone VARCHAR(20),
    Email VARCHAR(100),
    Address VARCHAR(200)
);


CREATE TABLE Departments (
    DepartmentID INT AUTO_INCREMENT PRIMARY KEY,
    DepartmentName VARCHAR(100) UNIQUE NOT NULL
);


CREATE TABLE DocumentTypes (
    DocumentTypeID INT AUTO_INCREMENT PRIMARY KEY,
    DocumentTypeName VARCHAR(100) UNIQUE NOT NULL
);


CREATE TABLE Documents (
    DocumentID INT AUTO_INCREMENT PRIMARY KEY,
    PatientID INT NOT NULL,
    DocumentTypeID INT NOT NULL,
    DepartmentID INT NOT NULL,
    UploadedBy INT NOT NULL,
    DocumentTitle VARCHAR(150) NOT NULL,
    FileName VARCHAR(255) NOT NULL,
    FilePath VARCHAR(255) NOT NULL,
    FileFormat VARCHAR(20) NOT NULL,
    UploadDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (PatientID) REFERENCES Patients(PatientID),
    FOREIGN KEY (DocumentTypeID) REFERENCES DocumentTypes(DocumentTypeID),
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID),
    FOREIGN KEY (UploadedBy) REFERENCES Users(UserID)
);


CREATE TABLE AuditLogs (
    LogID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NOT NULL,
    DocumentID INT,
    Action VARCHAR(50) NOT NULL,
    ActionDescription VARCHAR(255),
    ActionDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (UserID) REFERENCES Users(UserID),
    FOREIGN KEY (DocumentID) REFERENCES Documents(DocumentID)
);


INSERT INTO Departments (DepartmentName)
VALUES
('Laboratory'),
('Radiology'),
('Pharmacy'),
('Administration'),
('Medical Records');


INSERT INTO DocumentTypes (DocumentTypeName)
VALUES
('Patient Record'),
('Laboratory Report'),
('Radiology Report'),
('Prescription'),
('Consent Form'),
('Administrative Document');


INSERT INTO Users (FullName, Username, Password, Role, Email)
VALUES
('System Administrator', 'admin', 'admin123', 'Admin', 'admin@hospital.com'),
('Archive Staff', 'staff', 'staff123', 'Staff', 'staff@hospital.com');

SHOW TABLES;