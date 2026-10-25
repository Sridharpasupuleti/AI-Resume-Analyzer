# Backend

This is the Spring Boot backend. It requires Java 17 and Maven.

## MySQL configuration

Set `DB_USERNAME` and `DB_PASSWORD` in the environment before starting the application.
`DB_URL` is optional and defaults to `jdbc:mysql://localhost:3306/job_analyzer?createDatabaseIfNotExist=true`.
The JWT signing key is also required. This PowerShell example generates a
random key for the current session; keep it private and use the same value
while the app is running. The configured MySQL user must be allowed to create
the local development database and tables.

```powershell
$env:DB_USERNAME = "your-local-mysql-user"
$env:DB_PASSWORD = "your-local-mysql-password"
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$rng.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$rng.Dispose()

cd "C:\Users\sridh\eclipse-workspace\AI Resume Analyzer\backend"
mvn spring-boot:run
```

Flyway creates the `users` table on startup. Check the health endpoint at
`http://localhost:8080/api/v1/health`.
Flyway also creates the resume metadata table. Resume files are stored outside
the database under `backend/data/resumes` by default. Override the location
with `RESUME_STORAGE_DIR`; the upload limit defaults to 10 MB and can be
changed with `RESUME_MAX_FILE_SIZE`. Increase `RESUME_MAX_REQUEST_SIZE`
(default 11 MB) too when raising the file limit.

Run tests with:

```powershell
mvn test
```

## OpenAI analysis configuration

Resume analysis uses OpenAI by default. Set `LLM_API_KEY` in the environment
before starting the backend. `LLM_MODEL` defaults to `gpt-4o-mini`; the
request uses structured JSON output and has a 45-second read timeout. The
combined resume text and job description are limited to 100,000 characters.
The API key is never stored in source control.

Analysis sends the selected resume text and job description to the configured
OpenAI account. Review your provider account's data handling policies before
processing personal documents. To run the deterministic local analyzer
without making an external request, set `ANALYSIS_PROVIDER=mock`. OpenAI
failures are reported explicitly; they do not trigger a mock fallback.

For Eclipse, open **Run Configurations... → Environment** for the
`JobAnalyzerApplication` launch and add `LLM_API_KEY`, `DB_USERNAME`,
`DB_PASSWORD`, and `JWT_SECRET`. `ANALYSIS_PROVIDER` defaults to `openai`;
set it to `mock` only when intentionally testing without a provider call.
Keep API keys in the local launch configuration, not in source files.

## Authentication endpoints

- `POST /api/v1/auth/register` — accepts `fullName`, `email`, and `password`; returns a bearer token and user profile.
- `POST /api/v1/auth/login` — accepts `email` and `password`; returns a bearer token and user profile.
- `GET /api/v1/auth/me` — requires `Authorization: Bearer <accessToken>`.

Passwords must be at least 8 characters and no more than 72 UTF-8 bytes. The
token lifetime defaults to one hour and can be changed with
`JWT_EXPIRATION_MS`.

## Resume endpoints

All resume routes require a bearer token. Upload a PDF or DOCX using a
multipart form field named `file`:

- `POST /api/v1/resumes` — upload, validate, extract text, and save
- `GET /api/v1/resumes` — list resume metadata for the current user
- `GET /api/v1/resumes/{id}` — retrieve extracted text and metadata
- `GET /api/v1/resumes/{id}/file` — download the original file
- `DELETE /api/v1/resumes/{id}` — delete the resume

Files are private to their owner; requests for another user's resume return
not found.

## Job endpoints

All job routes require a bearer token. Create a job with JSON containing
`jobTitle` and `jobDescription` (up to 100,000 characters);
`companyName`, `jobUrl`, and `location` are optional:

- `POST /api/v1/jobs` — create a manually entered job
- `GET /api/v1/jobs` — list the current user's jobs
- `GET /api/v1/jobs/{id}` — retrieve a job
- `PUT /api/v1/jobs/{id}` — update a job
- `DELETE /api/v1/jobs/{id}` — delete a job

Jobs are private to their owner; requests for another user's job return
not found. Job-board scraping is not supported.

## Resume analysis endpoints

All analysis routes require a bearer token. Create an analysis by posting
`resumeId` and `jobId` to `/api/v1/analyses`; both records must belong to the
authenticated user:

- `POST /api/v1/analyses` — run and persist an analysis
- `GET /api/v1/analyses` — list saved analyses
- `GET /api/v1/analyses/{id}` — retrieve a saved analysis

The configured provider returns a structured match score, summary, matching
and missing skills and keywords, and recommendations. OpenAI is the default;
set `ANALYSIS_PROVIDER=mock` to use the deterministic local analyzer instead.
See [OpenAI analysis configuration](#openai-analysis-configuration) for
provider setup and data-handling details.
