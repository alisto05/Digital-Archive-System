# SyncPoint Hospital Digital Archive System

> A web-based digital archive system designed to help hospital staff securely store, manage, search, review, and access patient documents.

## Overview

**SyncPoint** is a Hospital Digital Archive System built to improve the accessibility and management of digital health records. The system provides separate functionality for patients, staff, and administrators while keeping the application, database, and document storage separated into services.

The project is organised as a full-stack application with:

- **Frontend:** Streamlit
- **Backend:** Java 17 + Spring Boot
- **Database:** MySQL 8
- **Containerisation:** Docker + Docker Compose
- **Document storage:** Persistent Docker volume
- **Security:** Spring Security and role-based access control

## Main Features

### Patients

- Register and log in
- Manage their profile
- Upload documents
- Search and access documents they are permitted to view
- Submit document requests
- View relevant document/request status information

### Staff

- Log in to the system
- Access patient-related information according to permissions
- Upload and manage documents
- Search the digital archive
- Review and process document requests
- Manage relevant patient/archive information

### Administrators

- Manage users and system administration functions
- Register/manage staff and patient accounts
- Review administrative requests
- Monitor archive activity and system information
- Perform administrative actions through protected endpoints

## System Architecture

```text
                         ┌─────────────────────────┐
                         │       Web Browser        │
                         └────────────┬────────────┘
                                      │
                                      ▼
                         ┌─────────────────────────┐
                         │   Streamlit Frontend    │
                         │       Port 8501         │
                         └────────────┬────────────┘
                                      │ HTTP API
                                      ▼
                         ┌─────────────────────────┐
                         │   Spring Boot Backend   │
                         │       Port 8080         │
                         │ Authentication / API    │
                         └──────────┬───────┬──────┘
                                    │       │
                         ┌──────────┘       └─────────────┐
                         ▼                                ▼
                ┌──────────────────┐             ┌──────────────────┐
                │     MySQL 8      │             │ Document Storage │
                │ syncpoint_archive│             │ Docker Volume    │
                └──────────────────┘             └──────────────────┘
```

## Project Structure

```text
Digital-Archive-System-main/
│
├── Backend/                  # Java Spring Boot REST API
│   ├── src/main/java/        # Controllers, DAOs, services and security
│   ├── src/test/             # Backend tests
│   ├── pom.xml               # Maven configuration
│   └── Dockerfile
│
├── Frontend/                 # Streamlit web application
│   ├── pages/                # Login, registration, dashboard and help pages
│   ├── assets/               # Images and application assets
│   ├── backend_api.py        # Backend API communication
│   ├── home.py               # Application home page
│   ├── requirements.txt
│   └── Dockerfile
│
├── db/                       # Database initialisation SQL scripts
├── .env.example              # Example environment configuration
├── .gitignore
├── docker-compose.yml        # Runs the complete application stack
└── README.md
```

## Requirements

For the recommended Docker setup, install:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Git

The Docker setup provides the required MySQL, Java and Python environments, so you do not need to install those runtimes separately to run the complete system with Docker Compose.

## Running the Project with Docker

### 1. Clone the repository

```bash
git clone <repository-url>
cd Digital-Archive-System-main
```

### 2. Create the environment file

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Update the values in `.env` before running the application.

Example:

```env
MYSQL_ROOT_PASSWORD=your-root-password
DB_USER=syncpoint
DB_PASSWORD=your-database-password

ADMIN_USERNAME=syncadmin
ADMIN_PASSWORD=your-admin-password
```

**Do not commit your real `.env` file or production passwords to GitHub.**

### 3. Build and start the services

```bash
docker compose up --build
```

The application contains three services:

| Service | Technology | Port |
|---|---|---:|
| `frontend` | Streamlit | `8501` |
| `backend` | Spring Boot / Java | `8080` |
| `db` | MySQL 8 | Internal Docker network |

### 4. Open the application

Open the frontend in your browser:

```text
http://localhost:8501
```

The backend is available locally on:

```text
http://localhost:8080
```

## Useful Docker Commands

Start the application in the background:

```bash
docker compose up -d
```

View running containers:

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs -f
```

View only the backend logs:

```bash
docker compose logs -f backend
```

Stop the application:

```bash
docker compose down
```

Rebuild the containers:

```bash
docker compose up --build
```

Stop the containers and remove the Docker volumes as well:

```bash
docker compose down -v
```

> **Warning:** Removing the volumes deletes the persisted MySQL database and stored archive documents.

## Database

The MySQL service creates the `syncpoint_archive` database. SQL files in the `db/` directory are mounted into MySQL's initialisation directory and are executed during database initialisation.

The database uses a persistent Docker volume named `db_data` so that database data is retained when containers are stopped.

## Document Storage

Uploaded archive documents are stored using the backend's persistent storage volume:

```text
archive_storage:/app/storage
```

This keeps uploaded documents separate from the application container itself and allows the documents to persist across container restarts.

## Backend API

The backend is implemented using **Spring Boot 3.3.4** and **Java 17**.

The backend contains components for:

- Authentication
- Patient management
- Staff management
- Administrator management
- Document management
- Document requests
- Document approvals/reviews
- Dashboard information
- Profile changes
- System status
- Access control and security

The API is consumed by the Streamlit frontend.

## Frontend

The frontend is implemented using **Streamlit** and provides the user-facing interface for the archive system.

The frontend includes pages for:

- Login
- Registration
- Dashboard
- Help
- Sign out

It communicates with the Spring Boot backend through HTTP API requests.

## Security

The project includes security mechanisms such as:

- Spring Security
- Authentication and session validation
- Password policy/validation
- Role-based access controls
- Protected administrative functionality
- Environment variables for database and administrator credentials

For a production deployment, passwords, secrets, database credentials, and other sensitive configuration values should be stored securely rather than committed to source control.

## Testing

Backend tests are located in:

```text
Backend/src/test/
```

Run the Maven test suite from the `Backend` directory with:

```bash
mvn test
```

When using Docker, the application can also be built through the supplied Dockerfiles and Docker Compose configuration.

## Development Without Docker

### Backend

Requirements:

- Java 17
- Maven
- MySQL 8

From the backend directory:

```bash
cd Backend
mvn spring-boot:run
```

### Frontend

Requirements:

- Python 3
- The packages listed in `Frontend/requirements.txt`

Install the dependencies:

```bash
cd Frontend
pip install -r requirements.txt
```

Run Streamlit:

```bash
streamlit run home.py
```

When running outside Docker, make sure the frontend's API URL is configured to point to the running backend.

## Environment Variables

The main environment variables used by the Docker Compose configuration are:

| Variable | Purpose |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `DB_USER` | Application database user |
| `DB_PASSWORD` | Application database password |
| `ADMIN_USERNAME` | Initial administrator username |
| `ADMIN_PASSWORD` | Initial administrator password |
| `SYNCPOINT_API_URL` | Frontend URL for the backend API |

## Contributing

1. Fork the repository.
2. Create a feature branch.
3. Make your changes.
4. Test the application.
5. Commit your changes with a clear message.
6. Push your branch and open a pull request.

## Project Status

This project is a software engineering / academic project for a Hospital Digital Archive System.
