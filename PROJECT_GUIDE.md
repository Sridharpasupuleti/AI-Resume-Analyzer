# AI Resume Analyzer — Project and Code Walkthrough

This guide explains how the project is organized and how a request travels
through the implemented backend. It is intended as a learning companion to the
source code: it follows files in execution order and explains the important
statements, fields, and methods. It does not repeat the meaning of every Java
import, brace, or blank line.

The guide describes the code currently in this repository. The project is
being built incrementally: the implemented product is a Spring Boot backend.
There is not yet a React frontend, job-application tracking module, or
deployment configuration.

## 1. What the application does

An authenticated user can:

1. Register and log in.
2. Upload a PDF or DOCX resume.
3. Have the backend extract text and store the file and resume metadata.
4. Save a job description manually.
5. Ask the backend to compare one of their resumes with one of their jobs.
6. Receive a structured analysis from OpenAI, or use the local mock provider.
7. List and retrieve saved analyses.

The main technologies are Java 17, Spring Boot, Spring Security, Spring Data
JPA/Hibernate, MySQL, Flyway, PDFBox, Apache POI, Jackson, and the JJWT
library.

## 2. Read the project in this order

For the quickest orientation, read:

1. [`backend/src/main/java/com/resumeanalyzer/JobAnalyzerApplication.java`](./backend/src/main/java/com/resumeanalyzer/JobAnalyzerApplication.java) — application entry point.
2. [`backend/src/main/resources/application.properties`](./backend/src/main/resources/application.properties) — runtime settings and environment variables.
3. [`backend/src/main/java/com/resumeanalyzer/config/SecurityConfig.java`](./backend/src/main/java/com/resumeanalyzer/config/SecurityConfig.java) — which routes need authentication.
4. One controller, such as [`AuthController.java`](./backend/src/main/java/com/resumeanalyzer/auth/controller/AuthController.java) — HTTP routes and request/response boundaries.
5. Its service — business logic and ownership rules.
6. Its repository and entity — database access and table mapping.
7. The corresponding Flyway migration — actual SQL schema.
8. The tests — executable examples of intended behavior.

The README files provide quick-start and API summaries. This file is the
longer learning-oriented walkthrough.

## 3. Repository map

```text
AI Resume Analyzer/
|-- README.md
|-- PROJECT_GUIDE.md                 this walkthrough
|-- MD Files/
|   |-- AGENTS.md                    contributor/agent conventions
|   `-- ARCHITECTURE.md              product architecture and phases
`-- backend/
    |-- pom.xml                      Maven dependencies/build
    |-- README.md                    backend setup and API notes
    `-- src/
        |-- main/
        |   |-- java/com/resumeanalyzer/
        |   |   |-- JobAnalyzerApplication.java
        |   |   |-- config/          Spring Security and beans
        |   |   |-- auth/            registration, login, JWT
        |   |   |-- user/            user table, repository and safe DTO
        |   |   |-- resume/          file upload, storage and extraction
        |   |   |-- job/             manually entered job descriptions
        |   |   |-- analysis/        OpenAI/mock comparison and saved result
        |   `-- resources/
        |       |-- application.properties
        |       `-- db/migration/    versioned database schema
        `-- test/java/               unit and HTTP/controller tests
```

### The common layers

Most feature modules use these roles:

- **Controller**: maps HTTP routes to Java methods, accepts validated DTOs,
  reads the authenticated user, and chooses the HTTP response.
- **DTO (data transfer object)**: defines the shape of incoming or outgoing
  JSON. Controllers do not expose JPA entities directly.
- **Service**: performs feature logic, ownership checks, orchestration, and
  entity-to-DTO mapping.
- **Repository**: Spring Data JPA interface; Spring creates its implementation.
- **Entity**: maps a Java object to a relational database table.
- **Migration**: SQL script Flyway applies to create or evolve the schema.
- **Exception handler**: converts known Java exceptions into a consistent API
  error response.

The normal path is:

```text
HTTP request
  -> Spring Security (if the route is protected)
  -> Controller
  -> DTO validation
  -> Service
  -> Repository / external provider
  -> Response DTO
  -> JSON or file HTTP response
```

## 4. How the app starts

### Entry point

[`JobAnalyzerApplication.java`](./backend/src/main/java/com/resumeanalyzer/JobAnalyzerApplication.java)

- The `@SpringBootApplication` annotation enables Spring Boot auto
  configuration, component scanning, and Spring configuration.
- `exclude = UserDetailsServiceAutoConfiguration.class` disables Spring
  Security's default generated development user. This application authenticates
  using its own JWT filter instead.
- `main` calls `SpringApplication.run`. Spring then builds the application
  context, creates configured beans, connects the database, applies Flyway
  migrations, and starts the HTTP server.

### Maven project

[`backend/pom.xml`](./backend/pom.xml)

- The Spring Boot parent version supplies managed dependency versions and
  standard Maven build behavior.
- `java.version` targets Java 17.
- `spring-boot-starter-web` provides Spring MVC and embedded HTTP-server
  support.
- `spring-boot-starter-data-jpa` provides JPA/Hibernate and Spring Data
  repositories.
- `spring-boot-starter-validation` enables Jakarta Bean Validation annotations
  such as `@NotBlank`, `@Size`, and `@Positive`.
- `spring-boot-starter-security` provides the Spring Security filter chain.
- `jjwt-api`, `jjwt-impl`, and `jjwt-jackson` create and verify signed JWTs.
- `flyway-core` and `flyway-mysql` apply the versioned MySQL migrations.
- PDFBox extracts text from PDF files; Apache POI reads DOCX files.
- MySQL Connector/J is the runtime JDBC driver.
- `spring-boot-starter-test` adds JUnit, Mockito, Spring MVC test support, and
  related testing utilities.
- The Spring Boot Maven plugin packages and runs the application.

Build/test commands are run from `backend`:

```powershell
mvn test
mvn spring-boot:run
```

## 5. Configuration and local setup

The source-of-truth settings are in
[`application.properties`](./backend/src/main/resources/application.properties).
`${NAME:default}` means “read environment variable/property `NAME`, or use the
default after the colon.”

| Setting | Default / purpose |
|---|---|
| `spring.application.name` | Application name used in logs and Spring context. |
| `DB_URL` | Defaults to a local MySQL `job_analyzer` database and asks the driver to create it if permitted. |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL credentials. These have no source-code defaults and must be configured to start the full backend. |
| `spring.jpa.hibernate.ddl-auto=validate` | Hibernate checks entity/table compatibility; it does not create the schema. Flyway owns schema changes. |
| `spring.jpa.open-in-view=false` | Prevents database access from leaking into HTTP response rendering after a service finishes. |
| `spring.flyway.enabled=true` | Applies pending `V<number>__description.sql` migrations during startup. |
| `RESUME_MAX_FILE_SIZE` | Resume upload maximum; defaults to 10 MB. |
| `RESUME_MAX_REQUEST_SIZE` | Multipart request maximum; defaults to 11 MB to include multipart overhead. |
| `RESUME_STORAGE_DIR` | Local resume file location; defaults to `./data/resumes` relative to the backend working directory. |
| `JWT_SECRET` | HMAC signing secret. Required; the application checks for at least 32 UTF-8 bytes. |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds; defaults to 3,600,000 (one hour). |
| `ANALYSIS_PROVIDER` | `openai` by default. Set exactly to `mock` to use the offline mock. |
| `LLM_API_KEY` | OpenAI API key. It is optional for app startup, but required when `openai` analysis is requested. |
| `LLM_MODEL` | OpenAI model; defaults to `gpt-4o-mini`. |
| `OPENAI_CHAT_COMPLETIONS_URL` | OpenAI-compatible Chat Completions endpoint. |
| `OPENAI_CONNECT_TIMEOUT_SECONDS` | Connection timeout; defaults to five seconds. |
| `OPENAI_READ_TIMEOUT_SECONDS` | Response/read timeout; defaults to 45 seconds. |

Set secrets in your local environment or Eclipse Run Configuration. Never put
actual credentials in this guide, Java code, or committed properties files.
To launch in Eclipse, the run configuration must use the
`JobAnalyzerApplication` main class and the `backend` Maven project, and its
Environment tab must provide `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`.
Provide `LLM_API_KEY` too to run real OpenAI analysis.

Example PowerShell environment setup (replace the database placeholders and
provide your own OpenAI key locally):

```powershell
$env:DB_USERNAME = "your-local-mysql-user"
$env:DB_PASSWORD = "your-local-mysql-password"
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$rng.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$rng.Dispose()
$env:LLM_API_KEY = "your-openai-api-key"
Set-Location "C:\path\to\AI Resume Analyzer\backend"
mvn spring-boot:run
```

`backend/data/` is ignored by Git because it contains uploaded/private files.
The default health endpoint is `GET /api/v1/health`.

## 6. Authentication and security

### Security configuration

[`SecurityConfig.java`](./backend/src/main/java/com/resumeanalyzer/config/SecurityConfig.java)

- `jwtService(...)` constructs the shared JWT service using the configured
  secret and expiration duration. Constructor checks in `JwtService` reject a
  weak/missing secret and non-positive token lifetime.
- `passwordEncoder()` returns BCrypt. Passwords are saved as one-way BCrypt
  hashes, never as the submitted plain text.
- `jwtFilterServletRegistration(...)` disables automatic servlet registration
  for the filter. The filter is instead placed once at the intended position
  in the Spring Security chain.
- `securityFilterChain(...)` disables CSRF, HTTP Basic, and form login because
  the API is stateless and uses bearer tokens.
- Stateless session policy means the server does not create a browser login
  session.
- The custom authentication entry point returns a JSON `ApiError` with HTTP
  401 when a protected route has no authenticated principal.
- Health, register, and login routes are public. All other routes require an
  authenticated principal.
- `addFilterBefore` runs JWT processing before Spring's username/password
  authentication filter.
- `writeUnauthorized(...)` writes a JSON 401, sets `Cache-Control: no-store`,
  and includes the request path. It does not expose a stack trace.

### Token and principal

[`JwtService.java`](./backend/src/main/java/com/resumeanalyzer/auth/service/JwtService.java)

- The constructor converts the secret into a JJWT HMAC signing key after
  validating its byte length and the expiration duration.
- `createToken(UserAccount)` delegates to the principal-based overload, so the
  signed token includes the account's ID and email.
- `createToken(UserPrincipal)` records the email as JWT subject, stores
  `userId` as a claim, sets issue and expiration timestamps, signs the claims,
  and serializes the result.
- `parsePrincipal(token)` verifies the token signature and expiry, reads the
  claims, requires a numeric user ID and nonblank subject, and returns a
  `UserPrincipal`.
- `getExpirationSeconds()` reports the configured lifetime in seconds for the
  login/register response.

[`UserPrincipal.java`](./backend/src/main/java/com/resumeanalyzer/auth/security/UserPrincipal.java)
is a small record containing the authenticated database user ID and email.
Protected controllers use this object; they must not trust an owner ID supplied
by a caller.

### JWT request filter

[`JwtAuthenticationFilter.java`](./backend/src/main/java/com/resumeanalyzer/auth/security/JwtAuthenticationFilter.java)

1. `doFilterInternal` reads the `Authorization` header.
2. If the header is absent or is not a bearer token, it leaves the request
   unauthenticated and continues the filter chain. Spring later permits or
   rejects the route according to the security rules.
3. For a bearer token, it extracts the token string and asks `JwtService` to
   verify it.
4. An invalid/expired token clears any security context, writes a JSON 401, and
   stops processing the request.
5. A valid token becomes a Spring `UsernamePasswordAuthenticationToken` with a
   `ROLE_USER` authority. Spring Security stores it in the request's
   `SecurityContext`.
6. The filter continues the chain, allowing a controller to receive the
   principal through `@AuthenticationPrincipal`.

### Authentication routes

[`AuthController.java`](./backend/src/main/java/com/resumeanalyzer/auth/controller/AuthController.java)

- `POST /api/v1/auth/register` validates a `RegisterRequest`, delegates to the
  service, and responds 201 Created.
- `POST /api/v1/auth/login` validates a `LoginRequest` and returns the
  service's token/profile response.
- `GET /api/v1/auth/me` receives the already authenticated principal and
  returns the corresponding safe user profile.

[`AuthService.java`](./backend/src/main/java/com/resumeanalyzer/auth/service/AuthService.java)

- Constructor injection supplies the user repository, password encoder, and
  token service.
- `register` first checks the password's UTF-8 byte length against BCrypt's
  72-byte limit. This matters because a multi-byte Unicode character may
  consume more than one byte.
- It normalizes the email using trim plus locale-independent lowercase.
- It checks for an existing account and raises a conflict exception if found.
- It trims the display name, BCrypt-encodes the password, saves a `UserAccount`,
  then returns a token and safe user DTO.
- `login` performs the same byte-limit and email-normalization checks. Unknown
  email and wrong password both raise the same `InvalidCredentialsException`
  so the endpoint does not reveal which case occurred.
- `getCurrentUser` looks up the user ID from the verified token and returns a
  safe profile; if that account was removed, it fails authentication.
- `authResponse` assembles a signed access token, bearer token type,
  expiration duration, and public user profile.
- `toUserResponse` deliberately omits the password hash.

[`RegisterRequest.java`](./backend/src/main/java/com/resumeanalyzer/auth/dto/RegisterRequest.java)
and [`LoginRequest.java`](./backend/src/main/java/com/resumeanalyzer/auth/dto/LoginRequest.java)
are immutable request records. Their Jakarta validation annotations reject
blank, malformed, or oversized values before the service is called.
Registration requires a password between 8 and 72 characters, while the
service adds the byte-accurate BCrypt limit check.

[`AuthResponse.java`](./backend/src/main/java/com/resumeanalyzer/auth/dto/AuthResponse.java)
holds the access token, token type, expiry seconds, and user profile.
[`UserResponse.java`](./backend/src/main/java/com/resumeanalyzer/user/dto/UserResponse.java)
contains only user ID, full name, and email.

The auth service's specific exceptions are:

- [`EmailAlreadyRegisteredException.java`](./backend/src/main/java/com/resumeanalyzer/auth/service/EmailAlreadyRegisteredException.java): maps duplicate registration to conflict.
- [`InvalidCredentialsException.java`](./backend/src/main/java/com/resumeanalyzer/auth/service/InvalidCredentialsException.java): maps invalid login/current-user lookup to unauthorized.
- [`PasswordTooLongException.java`](./backend/src/main/java/com/resumeanalyzer/auth/service/PasswordTooLongException.java): maps a byte length beyond BCrypt's limit to bad request.

## 7. User persistence and database migrations

### User entity and repository

[`UserAccount.java`](./backend/src/main/java/com/resumeanalyzer/user/entity/UserAccount.java)

- `@Entity` and `@Table` map this class to `users`.
- `id` is an auto-generated primary key.
- `fullName`, `email`, and `passwordHash` map to non-null database columns with
  explicit maximum lengths.
- `createdAt` and `updatedAt` record lifecycle timestamps.
- The protected no-argument constructor is required by JPA. The public
  constructor is used by application code to create an account.
- `@PrePersist` initializes both timestamps before the first insert.
- `@PreUpdate` refreshes `updatedAt` before a database update.
- Getters expose the fields to services and persistence code; API responses
  still use `UserResponse`, not the entity itself.

[`UserAccountRepository.java`](./backend/src/main/java/com/resumeanalyzer/user/repository/UserAccountRepository.java)
extends `JpaRepository`, which supplies standard CRUD methods. Spring Data
derives `findByEmail` and `existsByEmail` queries from their names.

### Flyway migrations

Migrations are applied in numeric order and should be treated as append-only:

- [`V1__create_users_table.sql`](./backend/src/main/resources/db/migration/V1__create_users_table.sql)
  creates the users table, primary key, email uniqueness constraint, and
  database-managed timestamp defaults.
- [`V2__create_resumes_table.sql`](./backend/src/main/resources/db/migration/V2__create_resumes_table.sql)
  creates resume metadata and extracted-text columns, an owner foreign key,
  unique opaque storage key, and user/created index.
- [`V3__create_jobs_table.sql`](./backend/src/main/resources/db/migration/V3__create_jobs_table.sql)
  creates company/title/description/URL/location/source fields, timestamps,
  owner foreign key, and user/created index.
- [`V4__create_resume_analyses_table.sql`](./backend/src/main/resources/db/migration/V4__create_resume_analyses_table.sql)
  stores the user, selected resume/job, searchable score and summary, complete
  structured JSON result, timestamps, score check, foreign keys, and lookup
  indexes.

The `ON DELETE CASCADE` user/resume/job foreign keys ensure dependent analysis
rows are removed if their referenced data is deleted. Resume file bytes are
stored on disk rather than in MySQL; the database keeps their generated key.

## 8. Resume upload and management

### HTTP controller

[`ResumeController.java`](./backend/src/main/java/com/resumeanalyzer/resume/controller/ResumeController.java)
is rooted at `/api/v1/resumes`. `@Validated` enables method-parameter
validation and every method accepts the authenticated `UserPrincipal`.

- `POST /api/v1/resumes` consumes multipart form data. The file must be in
  field `file`. The controller delegates validation/storage to the service and
  returns 201 Created with a `Location` header and `ResumeDetails`.
- `GET /api/v1/resumes` returns a list of lightweight `ResumeSummary` values.
- `GET /api/v1/resumes/{resumeId}` returns metadata and extracted text.
- `GET /api/v1/resumes/{resumeId}/file` asks the service for an owner-checked
  file resource, selects a PDF or DOCX media type, sets attachment download
  headers, and returns the original bytes.
- `DELETE /api/v1/resumes/{resumeId}` delegates deletion and returns 204 No
  Content.
- `@Positive` rejects IDs less than or equal to zero.

### Resume business flow

[`ResumeService.java`](./backend/src/main/java/com/resumeanalyzer/resume/service/ResumeService.java)
coordinates upload, database access, and the storage/extraction abstractions.

The constructor injects repositories, storage and extraction components, and
parses the configured maximum file size into bytes.

`upload(principal, file)` executes these steps in order:

1. Reject a missing or empty file.
2. Reject a file whose reported size exceeds the configured limit.
3. Sanitize the submitted file name: normalize Windows separators, keep only
   the basename, and reject empty, too-long, or control-character names.
4. Allow only `.pdf` and `.docx` extensions.
5. Read the uploaded bytes, reporting read failures explicitly.
6. Check the content signature. PDF must contain `%PDF-` near the start;
   DOCX must start with the ZIP file signature `PK` plus the expected marker
   bytes. The extension alone is not trusted.
7. Extract and normalize document text. An unreadable, protected, scanned, or
   empty document is rejected.
8. Write the original bytes using the storage abstraction and an opaque random
   key.
9. Build a `Resume` linked to the user ID from the verified principal, derive
   its title from the filename, and persist/flush the row.
10. If persistence fails after writing the file, try to delete the new file.
    A cleanup error is attached to the original error so it is not silently
    lost.

`list` queries rows for only the authenticated user's ID, newest first, then
maps each to a summary.

`get` uses an ID-plus-owner repository query. If there is no matching row, it
returns the same not-found response for nonexistent and foreign-owned IDs.

`download` repeats the ownership lookup before loading the opaque storage key.

`delete` owner-checks, deletes and flushes the metadata row, then deletes the
file. Flushing the database delete first reduces the risk that a storage
failure will roll back the metadata deletion after the file has already been
removed.

Helper methods:

- `findOwnedResume` enforces the user ID scope and maps missing records to
  `ResumeException.NOT_FOUND`.
- `readContent` translates upload byte-reading failures.
- `validateSignature` and `startsWithPdfHeader` prevent trusting only the
  submitted filename.
- `safeFileName` prevents path components from being used as the saved
  original filename.
- `extensionOf` limits accepted formats.
- `titleFrom` uses the basename before the extension, with `Resume` as a
  fallback title.
- `toSummary` omits extracted text from list responses.
- `toDetails` includes extracted text for a single resume.

### File storage boundary

[`ResumeFileStorage.java`](./backend/src/main/java/com/resumeanalyzer/resume/service/ResumeFileStorage.java)
defines three operations: store bytes, load a resource by storage key, and
delete a stored key. The interface makes the backing store replaceable without
changing resume business logic.

[`LocalResumeFileStorage.java`](./backend/src/main/java/com/resumeanalyzer/resume/service/LocalResumeFileStorage.java)
implements that contract locally:

- The configured directory is resolved to a normalized absolute path.
- `store` creates the directory, generates a UUID filename plus validated
  extension, and writes with `CREATE_NEW` so it cannot overwrite an existing
  file.
- `load` checks that the resolved path is a regular readable file before
  returning a Spring `Resource`.
- `delete` removes the file if present.
- `resolve` requires the final path's parent to equal the configured storage
  directory. A storage key cannot use path traversal to escape the directory.
- IO errors become `ResumeException.STORAGE_FAILURE`.

### Text extraction

[`ResumeTextExtractor.java`](./backend/src/main/java/com/resumeanalyzer/resume/service/ResumeTextExtractor.java)

- `extract` chooses a parser based on the normalized extension.
- The PDF path uses PDFBox's loader, rejects encrypted documents, and uses
  `PDFTextStripper`.
- The DOCX path opens an `XWPFDocument`, uses `XWPFWordExtractor`, and closes
  both resources with try-with-resources.
- `normalize` converts line endings to `\n`, removes trailing spaces on lines,
  joins lines, and strips whitespace around the full result.
- Empty extraction is explicitly rejected. Image-only/scanned files require
  OCR, which is not implemented.
- Parsing IO/runtime failures become a user-safe extraction error rather than
  being returned as a successful empty resume.

### Resume data objects

- [`Resume.java`](./backend/src/main/java/com/resumeanalyzer/resume/entity/Resume.java)
  maps owner, title, original name, file type, storage key, extracted text,
  and creation time to the `resumes` table. The owner is a lazy many-to-one
  relation. `@PrePersist` assigns the creation timestamp.
- [`ResumeRepository.java`](./backend/src/main/java/com/resumeanalyzer/resume/repository/ResumeRepository.java)
  derives the owner-filtered newest-first list query and owner-filtered
  single-row query.
- [`ResumeSummary.java`](./backend/src/main/java/com/resumeanalyzer/resume/dto/ResumeSummary.java)
  is the list response without large extracted text.
- [`ResumeDetails.java`](./backend/src/main/java/com/resumeanalyzer/resume/dto/ResumeDetails.java)
  adds extracted text for the detail/upload response.
- [`ResumeFileDownload.java`](./backend/src/main/java/com/resumeanalyzer/resume/dto/ResumeFileDownload.java)
  bundles a `Resource` with its safe original filename and file type for the
  controller's download response.
- [`ResumeException.java`](./backend/src/main/java/com/resumeanalyzer/resume/service/ResumeException.java)
  distinguishes invalid file, oversized file, not found, extraction failure,
  and storage failure. Its reason lets the global handler choose an HTTP
  status.

## 9. Job description management

### Routes

[`JobController.java`](./backend/src/main/java/com/resumeanalyzer/job/controller/JobController.java)
is rooted at `/api/v1/jobs`.

- `POST /api/v1/jobs`: validates a create DTO, calls the service, returns 201
  and a `Location` header.
- `GET /api/v1/jobs`: lists jobs owned by the authenticated user.
- `GET /api/v1/jobs/{jobId}`: retrieves one owner-scoped job.
- `PUT /api/v1/jobs/{jobId}`: validates and replaces the editable fields.
- `DELETE /api/v1/jobs/{jobId}`: owner-checks, deletes, and returns 204.

All methods get the owner from `@AuthenticationPrincipal`; no request DTO can
choose another user's `userId`.

### Service and data types

[`JobService.java`](./backend/src/main/java/com/resumeanalyzer/job/service/JobService.java)

- `create` uses the principal's user ID to obtain the owner reference, creates
  a `Job`, saves it, and maps it to `JobResponse`.
- `list` asks the repository only for rows with that owner ID and maps all
  results.
- `get` calls the shared owner-scoped lookup.
- `update` looks up the owned row first, changes the editable fields, saves,
  and maps the response.
- `delete` also looks up the owned row first; a foreign ID is never directly
  deleted.
- `findOwnedJob` turns “not found” and “belongs to another user” into the same
  `JobNotFoundException`.
- `toResponse` controls exactly which entity fields are returned.

[`Job.java`](./backend/src/main/java/com/resumeanalyzer/job/entity/Job.java)
maps to `jobs`. It relates a job to its owner and maps optional company, URL,
and location, required title/description, source, and timestamps.
`updateDetails` trims required text and converts blank optional text to null.
`@PrePersist` initializes both timestamps; `@PreUpdate` refreshes the update
timestamp. The source defaults to `MANUAL` and is not updated by the API.

[`JobRepository.java`](./backend/src/main/java/com/resumeanalyzer/job/repository/JobRepository.java)
contains owner-filtered newest-first list and owner-filtered ID lookup methods
in addition to standard JPA operations.

[`CreateJobRequest.java`](./backend/src/main/java/com/resumeanalyzer/job/dto/CreateJobRequest.java)
and [`UpdateJobRequest.java`](./backend/src/main/java/com/resumeanalyzer/job/dto/UpdateJobRequest.java)
define the accepted JSON fields. Title and description are required; company,
location, and URL are optional. Length constraints prevent oversized values,
and `@URL` validates a supplied URL.

[`JobResponse.java`](./backend/src/main/java/com/resumeanalyzer/job/dto/JobResponse.java)
returns the saved job fields and timestamps, without exposing the JPA entity.
[`JobNotFoundException.java`](./backend/src/main/java/com/resumeanalyzer/job/service/JobNotFoundException.java)
is translated to HTTP 404 by the global exception handler.

## 10. Resume analysis: the central user flow

### Request and response

[`CreateAnalysisRequest.java`](./backend/src/main/java/com/resumeanalyzer/analysis/dto/CreateAnalysisRequest.java)
requires positive, non-null `resumeId` and `jobId` fields.

Example:

```http
POST /api/v1/analyses
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "resumeId": 10,
  "jobId": 25
}
```

[`AnalysisResponse.java`](./backend/src/main/java/com/resumeanalyzer/analysis/dto/AnalysisResponse.java)
returns the saved analysis ID, source IDs and display titles, structured
result, and creation time.

[`ResumeAnalysisResult.java`](./backend/src/main/java/com/resumeanalyzer/analysis/dto/ResumeAnalysisResult.java)
defines the provider-independent result contract:

- `overallScore`: integer from zero to one hundred.
- `summary`: short human-readable result.
- `matchingSkills` and `missingSkills`.
- `matchingKeywords` and `missingKeywords`.
- `recommendations`.

### Controller and orchestration

[`AnalysisController.java`](./backend/src/main/java/com/resumeanalyzer/analysis/controller/AnalysisController.java)
is rooted at `/api/v1/analyses`:

- `POST` validates IDs, calls the service, and returns 201 with a Location
  header.
- `GET` lists only the caller's analyses.
- `GET /{analysisId}` retrieves one analysis only if the caller owns it.

[`AnalysisService.java`](./backend/src/main/java/com/resumeanalyzer/analysis/service/AnalysisService.java)
coordinates the workflow:

1. `create` looks up the resume using both the supplied resume ID and the
   authenticated user ID.
2. It looks up the job with both job ID and the same user ID.
3. If either is absent or belongs to someone else, the same not-found error
   is returned. The provider is not called with another user's data.
4. It passes the stored extracted resume text and job description to the
   `LlmAnalysisService` interface.
5. It validates the returned DTO using `AnalysisResultValidator`.
6. It creates and saves `ResumeAnalysis` with the authenticated owner, both
   source entities, and result.
7. `toResponse` builds an API response from the saved analysis.
- `list` and `get` use repository queries constrained by the authenticated
  owner.

The provider call is deliberately not wrapped in one long service transaction:
network calls can be slow, and holding a database transaction open throughout
them would tie up database resources unnecessarily. Repository operations
manage their own transactions.

### Provider abstraction

[`LlmAnalysisService.java`](./backend/src/main/java/com/resumeanalyzer/analysis/service/LlmAnalysisService.java)
has one method: `analyze(resumeText, jobDescription)`. `AnalysisService` knows
only this interface. Spring chooses the implementation from configuration;
the caller does not need provider-specific code.

### OpenAI provider — the real LLM call

[`OpenAiLlmAnalysisService.java`](./backend/src/main/java/com/resumeanalyzer/analysis/service/OpenAiLlmAnalysisService.java)
is the real provider implementation and is selected when
`ANALYSIS_PROVIDER=openai` (the default).

Important fields and statements:

- `MAX_INPUT_CHARACTERS` limits the combined resume and job description to
  100,000 characters before making a network request.
- `MAX_COMPLETION_TOKENS` caps the provider response size.
- The three schema maps define structured arrays for skills, keywords, and
  recommendations, with per-array item and string length limits.
- `RESULT_SCHEMA` describes all required JSON fields and their constraints.
  `additionalProperties=false` disallows unexpected fields.
- The constructor reads the API key, model, endpoint, and timeout values from
  configuration. It creates an HTTP client with a connection timeout and a
  Spring `RestClient` request factory with a read timeout.
- The alternate constructor takes a `RestClient` directly. This makes the
  provider class straightforward to test against a local mocked HTTP server.
- `analyze` first rejects a missing API key and oversized input, before
  sending anything externally.
- It builds a POST request to the configured OpenAI endpoint, adds the bearer
  API key only to the `Authorization` header, sends JSON, and requests JSON
  back.
- The request body contains a system prompt plus JSON-encoded source
  documents. Encoding documents as JSON keeps their text separate from
  instruction syntax. The prompt tells the model that resume/job text is
  untrusted data and not to obey embedded instructions or invent credentials.
- `response_format` requests strict JSON Schema output in the expected
  `ResumeAnalysisResult` shape.
- Provider/network `RestClientException`s become `AnalysisException` with a
  safe public message. Logs record the exception class, not the request body,
  resume text, or API key.
- `responseContent` rejects missing choices, empty content, refusals, and
  truncated output (`finish_reason=length`).
- Jackson parses the model's JSON into `ResumeAnalysisResult`.
- `AnalysisResultValidator` checks the parsed result before it leaves the
  provider adapter. Invalid JSON or invalid data becomes an explicit provider
  failure; it is not replaced with a successful mock answer.
- The private Java `record` types at the end model the OpenAI request and
  response envelope. `@JsonProperty` maps Java camelCase names to API
  underscore names such as `max_completion_tokens` and `response_format`.

The actual external HTTP operation is the `.post().uri(endpoint)...retrieve()`
chain inside `OpenAiLlmAnalysisService.analyze`. It runs only when a user
submits `POST /api/v1/analyses` and the selected implementation is OpenAI.
The service sends private resume text and job-description text to the
configured OpenAI account. Review that account's data handling terms before
using real personal documents.

### Local mock provider

[`MockLlmAnalysisService.java`](./backend/src/main/java/com/resumeanalyzer/analysis/service/MockLlmAnalysisService.java)
is selected only with `ANALYSIS_PROVIDER=mock`.

- `SKILLS` is a fixed dictionary of recognized technology/role terms.
- `STOP_WORDS` filters common words from keyword comparison.
- `TOKEN_PATTERN` splits text into candidate keywords.
- `analyze` lowercases both documents, identifies known skills mentioned in
  the job, avoids counting some parent skills when a more specific phrase is
  present, then partitions them into matching and missing lists.
- It extracts a limited set of job-description keywords, compares them with
  resume keywords, and reports present/missing keyword lists.
- It calculates a basic deterministic percentage and writes a summary that
  explicitly says this is not an AI assessment.
- `containsSkill` uses a regex with word boundaries, avoiding false matches
  such as matching “Java” inside “JavaScript”.
- `keywords` uses insertion order, lowercasing, stop-word removal, and a cap
  so result ordering is stable and list length is bounded.
- `recommendations` gives cautious suggestions, including “if accurate” for
  missing skills so a user is not told to claim experience they do not have.

The mock is a local fallback only in the sense that the developer selects it
explicitly. It is not automatically invoked when OpenAI is unconfigured or
fails.

### Result validation and errors

[`AnalysisResultValidator.java`](./backend/src/main/java/com/resumeanalyzer/analysis/service/AnalysisResultValidator.java)
checks:

- A result exists and its score is between 0 and 100.
- The summary is nonblank and no more than 1,000 characters.
- Every result list exists and is within its maximum item count.
- List values are non-null, nonblank, at most 500 characters, and not
  duplicated.
- Matching and missing entries do not overlap in the corresponding skills or
  keyword lists.

[`AnalysisException.java`](./backend/src/main/java/com/resumeanalyzer/analysis/service/AnalysisException.java)
distinguishes missing/foreign source records, invalid results, oversized
inputs, missing provider configuration, and provider failures. The reason is
used by the global handler to assign an appropriate HTTP response.

### Saved analysis

[`ResumeAnalysis.java`](./backend/src/main/java/com/resumeanalyzer/analysis/entity/ResumeAnalysis.java)
maps the result to `resume_analyses`.

- Relations link it to a user, resume, and job.
- The integer score and summary are kept in regular columns for efficient
  filtering/display.
- `result` is the full `ResumeAnalysisResult` object. Hibernate's JSON JDBC
  type maps it to MySQL's JSON column.
- The constructor copies score and summary from the result so the searchable
  fields agree with the structured response.
- `@PrePersist` assigns a creation timestamp.

[`ResumeAnalysisRepository.java`](./backend/src/main/java/com/resumeanalyzer/analysis/repository/ResumeAnalysisRepository.java)
provides standard JPA CRUD plus newest-first owner-scoped list and
owner-scoped detail queries.

## 11. Consistent API errors

[`ApiError.java`](./backend/src/main/java/com/resumeanalyzer/common/exception/ApiError.java)
defines the common error JSON: timestamp, HTTP status, reason phrase, message,
path, and optional field-validation messages.

[`GlobalExceptionHandler.java`](./backend/src/main/java/com/resumeanalyzer/common/exception/GlobalExceptionHandler.java)
uses `@RestControllerAdvice` to translate exceptions:

- Bean validation and constraint violations -> 400, with field messages.
- Missing/malformed JSON -> 400.
- Duplicate email and database constraint conflicts -> 409.
- Invalid credentials -> 401.
- Too-long bcrypt password -> 400.
- Missing/foreign job, resume, or analysis -> 404.
- Invalid resume file -> 400; too large -> 413; extraction failure -> 422;
  storage failure -> 500.
- Missing OpenAI configuration -> 503.
- OpenAI provider failure or invalid provider result -> 502.
- Unexpected exceptions -> logged server-side and a generic 500 response.

The handler does not return Java stack traces. Server logs should still be
protected because exceptions and operational details can be sensitive.

[`HealthController.java`](./backend/src/main/java/com/resumeanalyzer/common/health/HealthController.java)
returns `{"status":"UP"}` at `GET /api/v1/health`. It is a basic process/API
health response; it does not by itself prove that MySQL or OpenAI is currently
reachable.

## 12. Endpoint reference

All routes below are protected by a bearer JWT except the health, registration,
and login routes.

| Method | Route | Purpose |
|---|---|---|
| GET | `/api/v1/health` | Simple API health response. |
| POST | `/api/v1/auth/register` | Create an account and return a token. |
| POST | `/api/v1/auth/login` | Verify credentials and return a token. |
| GET | `/api/v1/auth/me` | Return the authenticated user's safe profile. |
| POST | `/api/v1/resumes` | Upload multipart `file` (PDF/DOCX). |
| GET | `/api/v1/resumes` | List own resume metadata. |
| GET | `/api/v1/resumes/{id}` | Get own resume and extracted text. |
| GET | `/api/v1/resumes/{id}/file` | Download own original file. |
| DELETE | `/api/v1/resumes/{id}` | Delete own resume and file. |
| POST | `/api/v1/jobs` | Create a manual job description. |
| GET | `/api/v1/jobs` | List own jobs. |
| GET | `/api/v1/jobs/{id}` | Get own job. |
| PUT | `/api/v1/jobs/{id}` | Replace editable fields of own job. |
| DELETE | `/api/v1/jobs/{id}` | Delete own job. |
| POST | `/api/v1/analyses` | Analyze own resume against own job and save result. |
| GET | `/api/v1/analyses` | List own analyses. |
| GET | `/api/v1/analyses/{id}` | Get own analysis result. |

For protected endpoints send:

```http
Authorization: Bearer <accessToken>
```

## 13. Tests and what they teach

Tests live under `backend/src/test/java`. They use JUnit and Mockito for
isolated business tests, and `@WebMvcTest`/MockMvc for HTTP/security behavior.
Provider tests use `MockRestServiceServer`; tests do not spend money or call
the real OpenAI service.

### Authentication

- [`AuthServiceTest.java`](./backend/src/test/java/com/resumeanalyzer/auth/service/AuthServiceTest.java)
  checks email normalization, BCrypt storage, duplicate registration, generic
  login failures, valid login, and the bcrypt byte-length limit.
- [`AuthControllerTest.java`](./backend/src/test/java/com/resumeanalyzer/auth/controller/AuthControllerTest.java)
  checks registration response, validation errors, protected current-user
  route, valid-token principal handling, and invalid bearer token rejection.

### Resume

- [`ResumeServiceTest.java`](./backend/src/test/java/com/resumeanalyzer/resume/service/ResumeServiceTest.java)
  checks secure upload sequence, owner assignment, unsupported extensions,
  file-signature mismatch, owner-scoped reads, and deletion ordering.
- [`ResumeTextExtractorTest.java`](./backend/src/test/java/com/resumeanalyzer/resume/service/ResumeTextExtractorTest.java)
  checks DOCX and PDF extraction, normalized output, invalid input, and empty
  documents.
- [`LocalResumeFileStorageTest.java`](./backend/src/test/java/com/resumeanalyzer/resume/service/LocalResumeFileStorageTest.java)
  checks write/load/delete and path traversal rejection.
- [`ResumeControllerTest.java`](./backend/src/test/java/com/resumeanalyzer/resume/controller/ResumeControllerTest.java)
  checks unauthenticated rejection and authenticated multipart upload response.

### Jobs

- [`JobServiceTest.java`](./backend/src/test/java/com/resumeanalyzer/job/service/JobServiceTest.java)
  checks owner assignment, field normalization, owner-only list/get/update/
  delete behavior.
- [`JobControllerTest.java`](./backend/src/test/java/com/resumeanalyzer/job/controller/JobControllerTest.java)
  checks authentication, create/Location response, DTO validation, list,
  not-found behavior, update, and delete response.

### Analysis and OpenAI

- [`AnalysisServiceTest.java`](./backend/src/test/java/com/resumeanalyzer/analysis/service/AnalysisServiceTest.java)
  checks that only the caller's resume and job are analyzed and saved, and
  that foreign resources or invalid analyzer output are rejected.
- [`AnalysisResultValidatorTest.java`](./backend/src/test/java/com/resumeanalyzer/analysis/service/AnalysisResultValidatorTest.java)
  checks valid results, invalid score/fields, and duplicate/conflicting
  evidence.
- [`MockLlmAnalysisServiceTest.java`](./backend/src/test/java/com/resumeanalyzer/analysis/service/MockLlmAnalysisServiceTest.java)
  checks predictable local skill/keyword comparisons and word-boundary
  behavior.
- [`OpenAiLlmAnalysisServiceTest.java`](./backend/src/test/java/com/resumeanalyzer/analysis/service/OpenAiLlmAnalysisServiceTest.java)
  checks request auth/schema/model, response parsing, missing key, input
  limits, provider rate limits, and malformed provider results without a real
  network call.
- [`AnalysisControllerTest.java`](./backend/src/test/java/com/resumeanalyzer/analysis/controller/AnalysisControllerTest.java)
  checks route authentication, create/list/get, required ID validation,
  owner-not-found behavior, and missing provider configuration response.

### Health

- [`HealthControllerTest.java`](./backend/src/test/java/com/resumeanalyzer/common/health/HealthControllerTest.java)
  checks that the health route returns `UP`.

Run the whole suite from the backend folder:

```powershell
mvn test
```

A passing test suite proves the exercised code paths work with test doubles.
It does not prove that local MySQL credentials are correct, that all migrations
have successfully applied to your database, or that your OpenAI account/key
can complete a live paid request.

## 14. A practical end-to-end learning sequence

Once the backend is running and migrations have completed:

1. Call `POST /api/v1/auth/register` with a name, email, and password. Save
   `accessToken` from the response.
2. Call `GET /api/v1/auth/me` with the bearer token. Observe how the JWT filter
   resolves the current user.
3. Upload a text-containing PDF or DOCX to `POST /api/v1/resumes` as multipart
   field `file`. Note the returned resume ID and extracted text.
4. Create a job with `POST /api/v1/jobs`. Note the returned job ID.
5. Ensure `LLM_API_KEY` is configured and call `POST /api/v1/analyses` with
   those two IDs. With the default provider this makes the OpenAI request.
6. Inspect the score, skills, keywords, and recommendations.
7. Call `GET /api/v1/analyses` and `GET /api/v1/analyses/{id}` to see persisted
   history.
8. Try the same IDs with a different user token. Owner-scoped queries should
   make the other user's records appear not found.

For an offline comparison without external provider traffic, set
`ANALYSIS_PROVIDER=mock` before starting the backend and repeat step 5.

## 15. Important boundaries and not-yet-implemented work

- This repository currently has no frontend; the REST API can be exercised
  with an HTTP client such as curl, Postman, or Eclipse/VS Code REST tooling.
- Job creation accepts manually entered descriptions. It does not scrape job
  sites.
- Resume parsing extracts text from PDF/DOCX; scanned images need OCR, which
  is not included.
- Resume files are stored locally. Multi-instance/production deployment
  should use a durable shared/object-storage implementation of
  `ResumeFileStorage`.
- OpenAI is an external paid service. A real analysis sends personal resume
  text and job-description content to the configured account.
- The mock analyzer is a deterministic heuristic, not generative AI and not
  professional hiring advice.
- The project does not yet include rate limiting, job application tracking,
  real LLM fallback/provider switching beyond the configured provider,
  frontend screens, Docker deployment, or cloud infrastructure.
- The architecture document is aspirational in places. When it describes
  planned modules or endpoints absent from the source, treat the Java code and
  migrations as the current implementation.

## 16. Short glossary

- **API**: a set of HTTP routes that another program can call.
- **Bearer token**: a signed access token sent in the `Authorization` header.
- **DTO**: a class/record defining API input or output data.
- **Entity**: a Java object mapped to a database table.
- **Flyway**: tool that applies ordered SQL database migrations.
- **JPA**: Java persistence API; Hibernate implements it here.
- **JWT**: signed token carrying the authenticated user's identity and expiry.
- **Mock**: a controlled substitute used for local/offline operation or tests.
- **Repository**: data-access interface implemented by Spring Data.
- **Service**: application/business logic between HTTP and persistence/provider.
- **Spring bean**: an object created and managed by the Spring application
  context.
- **Structured output**: provider response constrained to a known JSON shape,
  parsed into a typed Java record.
