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
- Authenticated PDF/DOCX resume upload, text extraction, listing, retrieval, download, and deletion
- Authenticated job description creation, listing, retrieval, updating, and deletion
- Authenticated, persisted resume-to-job analysis using OpenAI or a local mock analyzer

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
$env:ANALYSIS_PROVIDER = "openai"
$env:LLM_API_KEY = "your-openai-api-key"

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

## Resume API

All resume endpoints require a bearer token. Upload a PDF or DOCX file as a
multipart field named `file`:

- `POST /api/v1/resumes` — upload and extract resume text (maximum 10 MB)
- `GET /api/v1/resumes` — list your resume metadata
- `GET /api/v1/resumes/{id}` — retrieve your resume and extracted text
- `GET /api/v1/resumes/{id}/file` — download the original file
- `DELETE /api/v1/resumes/{id}` — delete your resume

Files are stored locally under `backend/data/resumes` by default. Set
`RESUME_STORAGE_DIR` to change the storage directory and
`RESUME_MAX_FILE_SIZE` to change the per-file limit (for example, `15MB`).
If increasing the file limit, increase `RESUME_MAX_REQUEST_SIZE` as needed
(default `11MB`) to allow for multipart request overhead.
Uploaded files are not committed to the repository.

## Jobs API

All job endpoints require a bearer token. Create a job with a manually entered
description:

```http
POST /api/v1/jobs
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "companyName": "Example Inc.",
  "jobTitle": "Java Engineer",
  "jobDescription": "Build and maintain Spring Boot services.",
  "jobUrl": "https://example.com/careers/java-engineer",
  "location": "Remote"
}
```

- `POST /api/v1/jobs` — create a job
- `GET /api/v1/jobs` — list your jobs
- `GET /api/v1/jobs/{id}` — retrieve one of your jobs
- `PUT /api/v1/jobs/{id}` — replace a job's editable fields
- `DELETE /api/v1/jobs/{id}` — delete a job

`jobTitle` and `jobDescription` are required (up to 100,000 characters).
Company, URL, and location are optional. Job descriptions are manually
entered; scraping is not supported.
Requests for another user's job return not found.

## Resume analysis API

Create an analysis by selecting one of your resumes and one of your jobs:

```http
POST /api/v1/analyses
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "resumeId": 10,
  "jobId": 25
}
```

- `POST /api/v1/analyses` — analyze and save a resume/job match
- `GET /api/v1/analyses` — list your saved analyses
- `GET /api/v1/analyses/{id}` — retrieve one of your saved analyses

Analysis calls OpenAI's Chat Completions API using `gpt-4o-mini` by default.
Set `LLM_API_KEY` in your environment before starting the backend; optionally
set `LLM_MODEL` to select another compatible model. The selected resume text
and job description are sent to OpenAI for analysis. Do not upload documents
unless you are comfortable processing them through your configured OpenAI
account. Both source records must belong to the authenticated user.

For local development without a provider key, set `ANALYSIS_PROVIDER=mock`.
That uses a deterministic keyword-based comparison and is not an AI
assessment. The mock provider is not used as a silent fallback when OpenAI is
selected but unavailable or unconfigured.

## Project documentation

- [Architecture and implementation phases](./MD%20Files/ARCHITECTURE.md)
- [Agent contribution instructions](./MD%20Files/AGENTS.md)
- [Backend setup details](./backend/README.md)

## Project status

The project is under active development. Resume management, job management,
and configurable OpenAI/mock resume analysis are implemented in the backend;
the React frontend and deployment are planned future phases.
