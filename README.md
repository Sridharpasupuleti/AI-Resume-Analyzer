# AI Resume Analyzer

A full-stack project, starting with a Java 17, Spring Boot, MySQL backend for
managing job applications and analyzing resumes against job descriptions.
The application is being built incrementally; this repository currently
contains the backend foundation and authentication API.

## Current backend features

- Spring Boot REST API with a health endpoint
- MySQL persistence managed with Flyway migrations
- User registration and login with BCrypt password hashing
- Stateless JWT authentication
- Authenticated current-user endpoint
- Request validation and consistent API error responses

## Requirements

- Java 17
- Maven 3.9+
- MySQL 8+

## Configure and run the backend

Set MySQL credentials and generate a JWT signing key in PowerShell. The key
below is randomly generated for the current shell session; do not commit it
or share it.

```powershell
$env:DB_USERNAME = "your-local-mysql-user"
$env:DB_PASSWORD = "your-local-mysql-password"
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$rng.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$rng.Dispose()

cd backend
mvn spring-boot:run
```

The default database URL is
`jdbc:mysql://localhost:3306/job_analyzer?createDatabaseIfNotExist=true`.
The MySQL user must be allowed to create the database and tables. Flyway
creates the schema on startup.

Health check: <http://localhost:8080/api/v1/health>

Run backend tests:

```powershell
cd backend
mvn test
```

## Authentication API

### Register

`POST /api/v1/auth/register`

```json
{
  "fullName": "Casey Example",
  "email": "casey@example.com",
  "password": "choose-a-strong-password"
}
```

### Login

`POST /api/v1/auth/login`

```json
{
  "email": "casey@example.com",
  "password": "choose-a-strong-password"
}
```

Registration and login return a bearer access token and a user profile. Use
the token to call the authenticated endpoint:

`GET /api/v1/auth/me`

```http
Authorization: Bearer <access-token>
```

Passwords must be at least 8 characters and no more than 72 UTF-8 bytes.
Tokens expire after one hour by default; configure `JWT_EXPIRATION_MS` to
change the lifetime.

## Project documentation

- [Architecture and implementation phases](./MD%20Files/ARCHITECTURE.md)
- [Agent contribution instructions](./MD%20Files/AGENTS.md)
- [Backend setup details](./backend/README.md)

## Project status

The project is under active development. Resume management, job management,
resume analysis, the React frontend, and deployment are planned future phases.
