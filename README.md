# Syncpoint Archive Backend

A Spring Boot REST API that connects to the `syncpoint_archive` MySQL database and
wraps the stored procedures/triggers you supplied (patient & staff registration,
login tracking, document upload/review, document requests, and profile-change
approval workflows).

## Requirements
- Java 17+
- Maven 3.8+
- A running MySQL instance with the `syncpoint_archive` schema, tables, triggers,
  and stored procedures already created (the triggers/procedures you shared assume
  the base tables — `users`, `patients`, `documents`, `audit_logs`, etc. — already exist).

## Configure the database connection
Set these environment variables before running (defaults shown in parentheses):

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=your_password
```

These map to `src/main/resources/application.properties`. Never commit real
credentials — use environment variables or a secrets manager in production.

## Run it
```bash
mvn spring-boot:run
```
The API starts on `http://localhost:8080`.

## Build a runnable jar
```bash
mvn clean package
java -jar target/syncpoint-archive-backend-1.0.0.jar
```

## Endpoints

| Method | Path                                   | Calls                         |
|--------|-----------------------------------------|--------------------------------|
| POST   | /api/patients/register                 | sp_register_patient           |
| POST   | /api/staff/register                    | sp_register_staff             |
| POST   | /api/auth/login                        | sp_record_login_attempt       |
| POST   | /api/auth/logout?loginId=123            | sp_record_logout              |
| POST   | /api/search                            | sp_record_search              |
| POST   | /api/documents/upload                  | sp_upload_document            |
| PUT    | /api/documents/{id}/review             | sp_review_document            |
| POST   | /api/documents/request                 | sp_request_document           |
| POST   | /api/profile-changes                   | sp_submit_change_request      |
| PUT    | /api/profile-changes/{id}/resolve      | sp_resolve_change_request     |

All the triggers you defined (audit logging, `last_login_at` updates, etc.) fire
automatically as side effects of these procedure calls — the Java code never
writes to `audit_logs` directly.

## Notes / things to adapt before production use
- **Passwords**: registration and login hash/verify with BCrypt
  (`spring-security-crypto`). Passwords are accepted as plaintext over the wire —
  make sure this API only ever runs behind HTTPS.
- **Auth/session**: `/api/auth/login` currently returns the raw `userId` and
  `role`. Replace this with a real session token or JWT before exposing the API
  publicly, and add an auth filter to protect the staff-only endpoints
  (document review, profile-change resolution, document requests).
- **File storage**: `/api/documents/upload` only records document metadata; you
  still need to actually store the uploaded file (disk, S3, etc.) and pass its
  location in as `storageKey` before calling this endpoint, or extend the
  controller to accept a multipart file directly.
- **Authorization**: nothing here checks that the caller is allowed to act on a
  given patient/document (e.g. that a PATIENT role can only see their own
  records) — add that check in a service layer or filter before going live.
