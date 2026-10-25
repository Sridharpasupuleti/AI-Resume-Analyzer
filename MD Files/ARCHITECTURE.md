# AI Job Application & Resume Analyzer --- Architecture & Build Plan

## 1. Purpose

Build a production-style full-stack application that helps a user manage
resumes and jobs, compare a resume against a job description, generate
an explainable match score, identify matching/missing skills and
keywords, produce recommendations, and track applications.

The project should demonstrate strong **Java backend engineering**
rather than being only an LLM demo.

### Stack

-   Java
-   Spring Boot
-   Spring Security
-   Spring Data JPA / Hibernate
-   MySQL
-   Redis
-   React.js
-   REST APIs
-   LLM integration behind an application interface
-   Maven
-   Git/GitHub
-   Docker later

------------------------------------------------------------------------

# 2. Product Workflow

``` text
User
  |
  v
React Frontend
  |
  v
Spring Boot REST API
  |
  +--------------------------+
  |                          |
  v                          v
MySQL                    LLM Provider
  |                          |
  |                          v
  |                    Structured Analysis
  |                          |
  +-------------<------------+
                |
                v
          Analysis Result
                |
                v
            React UI
```

The user journey is:

``` text
Register/Login
      |
      v
Upload Resume
      |
      v
Create/Paste Job Description
      |
      v
Select Resume + Job
      |
      v
Analyze
      |
      v
LLM + deterministic validation/scoring
      |
      v
Match Score
      |
      +--> Matching Skills
      +--> Missing Skills
      +--> Keywords
      +--> Recommendations
      |
      v
Save Analysis
      |
      v
Track Application
```

------------------------------------------------------------------------

# 3. Architecture Principles

Use a **modular monolith** initially.

Do not start with microservices. The application should have clear
module boundaries so services can be extracted later if there is a real
need.

Core principles:

1.  Controllers stay thin.
2.  Business logic belongs in services/application logic.
3.  Persistence belongs behind repositories.
4.  REST APIs use DTOs rather than exposing JPA entities.
5.  Every user-owned resource must enforce ownership.
6.  LLM calls are isolated behind an interface.
7.  External input is validated.
8.  Secrets are supplied through environment variables.
9.  Expensive operations are rate limited.
10. Important business logic has automated tests.
11. Do not introduce infrastructure merely because it is fashionable.

------------------------------------------------------------------------

# 4. Repository Structure

Recommended final structure:

``` text
ai-job-application-resume-analyzer/
│
├── MD Files/
│   ├── AGENTS.md
│   └── ARCHITECTURE.md
├── README.md
├── .env.example
├── .gitignore
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/resumeanalyzer/
│       │   └── resources/
│       └── test/
│
├── frontend/
│   ├── package.json
│   └── src/
│
└── docker/
    └── ...
```

Backend modules:

``` text
com.resumeanalyzer
├── auth
├── user
├── resume
├── job
├── analysis
├── application
├── ai
├── common
└── config
```

Within a feature:

``` text
feature/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
└── exception/
```

Do not create a huge unrelated `utils` package.

------------------------------------------------------------------------

# 5. Backend Layering

The intended flow is:

``` text
HTTP Request
     |
     v
Controller
     |
     v
Request DTO validation
     |
     v
Application/Service layer
     |
     +----> Domain/business rules
     |
     +----> Repository
     |
     +----> External integration
     |
     v
Response DTO
     |
     v
HTTP Response
```

Controllers should not:

-   contain database queries
-   contain LLM prompts
-   contain scoring algorithms
-   contain complex business rules

------------------------------------------------------------------------

# 6. Authentication & Authorization

Use:

-   Spring Security
-   BCrypt password hashing
-   JWT authentication

Endpoints:

``` text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

Authentication flow:

``` text
Login
  |
  v
Validate credentials
  |
  v
Generate JWT
  |
  v
React stores authentication state
  |
  v
Request contains JWT
  |
  v
Spring Security filter
  |
  v
SecurityContext
  |
  v
Controller
```

Rules:

``` text
/api/v1/auth/** -> public

/api/v1/resumes/**      -> authenticated
/api/v1/jobs/**         -> authenticated
/api/v1/analyses/**     -> authenticated
/api/v1/applications/** -> authenticated
```

A resource ID must never be treated as authorization.

For example, if user A requests another user's resume ID, the API must
return an appropriate not-found/forbidden response rather than leaking
the resource.

------------------------------------------------------------------------

# 7. User Module

Suggested entity:

``` text
User
----
id
name
email
passwordHash
createdAt
updatedAt
```

Constraints:

-   email must be unique
-   password is never returned by the API
-   password is never logged
-   email must be normalized consistently

A user owns:

``` text
Resumes
Jobs
Analyses
Applications
```

------------------------------------------------------------------------

# 8. Resume Module

Responsibilities:

-   upload resume
-   validate file
-   store file
-   extract text
-   normalize text
-   store metadata
-   support resume versions

Suggested entity:

``` text
Resume
------
id
userId
title
originalFileName
fileType
storageKey
parsedText
version
createdAt
updatedAt
```

Initial supported formats:

-   PDF
-   DOCX

Initial storage can be local, behind:

``` java
FileStorageService
```

Later it can be replaced with object storage without changing the rest
of the application.

Workflow:

``` text
Upload
  |
  v
Validate extension/content/size
  |
  v
Store file
  |
  v
Extract text
  |
  v
Normalize text
  |
  v
Persist metadata + parsed text
```

Do not store huge binary files directly in MySQL unless there is a
specific reason.

------------------------------------------------------------------------

# 9. Job Module

Suggested entity:

``` text
Job
---
id
userId
companyName
jobTitle
jobDescription
jobUrl
location
source
createdAt
updatedAt
```

Endpoints:

``` text
POST   /api/v1/jobs
GET    /api/v1/jobs
GET    /api/v1/jobs/{id}
PUT    /api/v1/jobs/{id}
DELETE /api/v1/jobs/{id}
```

The first version should support pasted/manual job descriptions.

Do not build scraping or browser automation in the first version.

------------------------------------------------------------------------

# 10. Analysis Module

This is the core feature.

Request:

``` json
{
  "resumeId": 10,
  "jobId": 25
}
```

Flow:

``` text
AnalysisController
       |
       v
AnalysisService
       |
       +--> verify authenticated user
       |
       +--> load Resume
       |
       +--> load Job
       |
       +--> prepare text
       |
       +--> call LlmAnalysisService
       |
       +--> validate response
       |
       +--> verify/derive score
       |
       +--> persist analysis
       |
       v
AnalysisResponse
```

Suggested entity:

``` text
ResumeAnalysis
--------------
id
userId
resumeId
jobId
overallScore
summary
resultJson
createdAt
updatedAt
```

Keep important searchable fields as columns. JSON/JSONB can hold the
richer analysis result.

------------------------------------------------------------------------

# 11. LLM Abstraction

Never couple the whole application directly to a provider SDK.

Define:

``` java
public interface LlmAnalysisService {

    ResumeAnalysisResult analyze(
        String resumeText,
        String jobDescription
    );
}
```

Implementations can include:

``` text
LlmAnalysisService
       |
       +-- RealLlmAnalysisService
       |
       +-- MockLlmAnalysisService
```

Benefits:

-   unit testing without API calls
-   easier provider replacement
-   reduced coupling
-   predictable application behavior

Environment variables:

``` text
LLM_API_KEY
LLM_MODEL
```

Never hard-code keys.

------------------------------------------------------------------------

# 12. Structured LLM Output

The LLM must return structured data, not only free-form prose.

Target shape:

``` json
{
  "overallScore": 82,
  "summary": "Strong backend alignment with some gaps.",
  "matchedSkills": [
    "Java",
    "Spring Boot",
    "REST APIs",
    "MySQL"
  ],
  "missingSkills": [
    "AWS",
    "Kubernetes"
  ],
  "keywords": {
    "present": [
      "Spring Boot",
      "REST API"
    ],
    "missing": [
      "AWS"
    ]
  },
  "recommendations": [
    "Highlight backend performance improvements.",
    "Add measurable project outcomes."
  ]
}
```

Backend responsibilities:

1.  Parse the response.
2.  Validate schema.
3.  Validate ranges.
4.  Reject malformed output.
5.  Persist only validated data.

Never blindly trust LLM output.

The LLM must not:

-   access the database
-   modify users
-   modify applications
-   execute arbitrary backend commands
-   determine authorization

------------------------------------------------------------------------

# 13. Scoring Strategy

Do not make the match score an unexplained number.

A first version can use:

``` text
50% skill alignment
20% experience alignment
15% keyword alignment
15% education/certification alignment
```

The exact weights should be configurable/refinable.

Where possible, combine:

``` text
Deterministic checks
        +
LLM semantic analysis
        =
Final analysis
```

The score is an estimate. It should not be described as a guaranteed ATS
score.

------------------------------------------------------------------------

# 14. Application Tracking Module

Suggested entity:

``` text
JobApplication
--------------
id
userId
jobId
resumeId
status
appliedAt
notes
createdAt
updatedAt
```

Statuses:

``` text
SAVED
APPLIED
SCREENING
INTERVIEW
OFFER
REJECTED
WITHDRAWN
```

Endpoints:

``` text
POST   /api/v1/applications
GET    /api/v1/applications
GET    /api/v1/applications/{id}
PATCH  /api/v1/applications/{id}/status
DELETE /api/v1/applications/{id}
```

------------------------------------------------------------------------

# 15. DTO Strategy

Do not expose entities directly.

Example:

``` text
CreateJobRequest
UpdateJobRequest
JobResponse
```

Flow:

``` text
CreateJobRequest
       |
       v
Controller
       |
       v
Service
       |
       v
Job Entity
       |
       v
Repository
```

Response:

``` text
Entity
  |
  v
Mapper
  |
  v
JobResponse
  |
  v
Controller
```

This keeps database design independent from API contracts.

------------------------------------------------------------------------

# 16. Database

Initial tables:

``` text
users
resumes
jobs
resume_analyses
job_applications
```

Relationships:

``` text
User
 |
 +----< Resume
 |
 +----< Job
 |
 +----< ResumeAnalysis
 |
 +----< JobApplication

Resume
 |
 +----< ResumeAnalysis

Job
 |
 +----< ResumeAnalysis
 |
 +----< JobApplication
```

Use:

-   primary keys
-   foreign keys
-   unique constraints
-   indexes
-   created/updated timestamps

Useful indexes:

``` text
users.email
resumes.user_id
jobs.user_id
resume_analyses.user_id
resume_analyses.resume_id
resume_analyses.job_id
job_applications.user_id
job_applications.status
```

Use database migrations rather than relying on uncontrolled schema
creation in production.

------------------------------------------------------------------------

# 17. REST API Convention

Use:

``` text
/api/v1/...
```

Main resources:

``` text
/api/v1/auth
/api/v1/resumes
/api/v1/jobs
/api/v1/analyses
/api/v1/applications
```

Use appropriate HTTP methods:

``` text
POST   create
GET    retrieve/list
PUT    replace/update
PATCH  partial update
DELETE delete
```

Use consistent response/error formats.

------------------------------------------------------------------------

# 18. Error Handling

Use:

``` java
@RestControllerAdvice
```

Example:

``` json
{
  "timestamp": "2026-10-08T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Job title is required",
  "path": "/api/v1/jobs"
}
```

Handle:

-   validation errors
-   authentication errors
-   authorization errors
-   not found
-   duplicate resources
-   invalid files
-   LLM failures
-   timeouts
-   database errors
-   unexpected errors

Never return stack traces to the client.

------------------------------------------------------------------------

# 19. React Architecture

Suggested structure:

``` text
frontend/src/
├── api/
├── components/
├── pages/
├── layouts/
├── hooks/
├── context/
├── services/
├── utils/
├── types/
├── routes/
└── App.jsx
```

Pages:

``` text
Login
Register
Dashboard
Resumes
ResumeDetails
Jobs
JobDetails
Analysis
Applications
Profile
```

Frontend responsibilities:

-   presentation
-   form validation
-   loading/error states
-   API communication
-   authentication state
-   navigation

Do not move backend business rules into React.

------------------------------------------------------------------------

# 20. Dashboard

Display:

-   total jobs
-   saved jobs
-   applications
-   interviews
-   offers
-   rejected applications
-   recent analyses

Keep the first dashboard simple.

------------------------------------------------------------------------

# 21. Analysis UI

When the user clicks **Analyze Resume**:

``` text
Select Resume
      +
Select Job
      |
      v
Analyze
      |
      v
Loading
      |
      v
Result
```

Result screen:

``` text
Overall Match Score
        |
        +-- Summary
        +-- Matching Skills
        +-- Missing Skills
        +-- Keywords
        +-- Recommendations
```

Also display analysis history.

------------------------------------------------------------------------

# 22. Redis

Redis is useful for:

1.  API rate limiting.
2.  Carefully selected caching.

Most important initial use:

``` text
POST /api/v1/analyses
```

because analysis may call an external paid LLM.

Example configurable limit:

``` text
10 analyses / user / hour
```

Do not hard-code the limit into business logic.

Redis should not be used simply because the resume is a backend project.

------------------------------------------------------------------------

# 23. Performance

First optimize:

-   database indexes
-   pagination
-   avoiding N+1 queries
-   rate limiting expensive endpoints
-   appropriate Redis usage
-   connection pooling
-   LLM timeout handling

Do not prematurely introduce Kafka/RabbitMQ.

Later, if analysis becomes slow or traffic grows:

``` text
POST /analyses
      |
      v
Create analysis job
      |
      v
Queue
      |
      v
Worker
      |
      v
LLM
      |
      v
Database
```

The UI can poll or receive completion events later.

------------------------------------------------------------------------

# 24. Security

Never commit:

``` text
API keys
JWT secrets
database passwords
real .env files
```

Provide:

``` text
.env.example
```

Log carefully.

Never log:

-   passwords
-   tokens
-   API keys
-   complete resumes
-   unnecessary sensitive personal information

Validate upload size/type.

Do not trust filenames.

Prevent path traversal in file storage.

------------------------------------------------------------------------

# 25. Testing

## Backend unit tests

Test:

-   scoring
-   services
-   validation
-   DTO behavior
-   LLM response parsing
-   authorization logic

## Integration tests

Test:

``` text
Controller
  ->
Service
  ->
Repository
```

Use a real MySQL-compatible test environment where practical.

## LLM tests

Use `MockLlmAnalysisService`.

Do not make ordinary unit tests call the real paid LLM.

## Frontend tests

Test:

-   login
-   registration
-   upload
-   job creation
-   analysis result rendering
-   application status changes

------------------------------------------------------------------------

# 26. Configuration

Example environment variables:

``` text
DB_URL
DB_USERNAME
DB_PASSWORD

JWT_SECRET

LLM_API_KEY
LLM_MODEL

REDIS_HOST
REDIS_PORT
```

Use separate local/test/production configuration.

------------------------------------------------------------------------

# 27. Docker

Add Docker after local development is working.

Target:

``` text
Browser
   |
React
   |
Spring Boot
  / \
MySQL Redis
   |
 LLM
```

Possible containers:

``` text
frontend
backend
mysql
redis
```

Do not containerize complexity before the application works locally.

------------------------------------------------------------------------

# 28. Logging & Observability

Record:

-   request ID
-   endpoint
-   execution time
-   success/failure
-   external LLM latency
-   relevant error context

Do not record secrets or full sensitive documents.

Later add metrics for:

``` text
analysis latency
LLM failures
rate-limit rejections
API response times
database performance
```

------------------------------------------------------------------------

# 29. Development Phases

The project must be built one phase at a time.

## Phase 0 --- Planning

Maintain:

``` text
MD Files/AGENTS.md
README.md
MD Files/ARCHITECTURE.md
```

Do not build business features.

Acceptance:

-   repository has clear instructions
-   architecture is documented
-   initial structure is clear

------------------------------------------------------------------------

## Phase 1 --- Backend Foundation

Implement:

-   Spring Boot
-   Maven
-   MySQL configuration
-   package structure
-   health endpoint
-   validation
-   global exception handler
-   logging

Acceptance:

``` text
Backend starts.
Database connects.
GET /api/v1/health -> 200.
Tests pass.
```

Implementation progress: the Java 17 backend foundation, MySQL configuration,
health endpoint, validation error handling, and logging are in place under
`backend/`. The health endpoint test passes. A local MySQL service is running,
but the application-to-database connection has not yet been verified because
`DB_USERNAME` and `DB_PASSWORD` are not configured in the current environment.

------------------------------------------------------------------------

## Phase 2 --- Authentication

Implement:

-   User entity
-   registration
-   login
-   BCrypt
-   JWT
-   Spring Security
-   current user endpoint

Acceptance:

``` text
Registration works.
Login works.
Protected endpoints reject unauthenticated users.
Authenticated user is resolved correctly.
```

Implementation progress: registration, login, BCrypt password hashing, JWT
authentication, stateless Spring Security, and `GET /api/v1/auth/me` are
implemented. Flyway creates the MySQL users table, and unit/web tests cover
password handling, input validation, and protected endpoint behavior. The
MySQL migration itself still needs to be verified in an environment with
configured database credentials.

------------------------------------------------------------------------

## Phase 3 --- Resume Management

Implement:

-   Resume entity
-   upload
-   file validation
-   storage abstraction
-   PDF/DOCX extraction
-   listing
-   retrieval
-   deletion
-   ownership checks

Acceptance:

``` text
User uploads a resume.
Text is extracted.
Metadata persists.
Only the owner can access it.
```

Implementation progress: authenticated PDF/DOCX upload, content checks,
10 MB configurable limit, text extraction and normalization, local file
storage behind an interface, owner-scoped listing/retrieval/download/deletion,
and Flyway persistence are implemented. Automated tests cover extraction,
validation, upload ownership, and endpoint authentication.

------------------------------------------------------------------------

## Phase 4 --- Job Management

Implement:

-   Job entity
-   create
-   list
-   retrieve
-   update
-   delete
-   ownership checks

Do not implement scraping.

Implementation progress: authenticated job creation, listing, retrieval,
update, and deletion are implemented for manually entered job descriptions.
Job access is scoped to the authenticated owner, and Flyway creates the jobs
table with a user foreign key. Request validation and controller/service tests
cover the API and CRUD behavior.

------------------------------------------------------------------------

## Phase 5 --- Analysis Without Real LLM

Implement:

``` text
LlmAnalysisService
MockLlmAnalysisService
```

Build:

-   analysis request
-   DTOs
-   result validation
-   persistence
-   complete business workflow

This phase proves the architecture before adding external API costs.

Implementation progress: authenticated analysis creation verifies ownership
of both source records, calls an `LlmAnalysisService` backed by a deterministic
keyword-based mock, validates and persists the structured result, and provides
owner-scoped analysis history and retrieval. The score and recommendations
are explicitly basic keyword heuristics, not an AI assessment.

The mock provider is selected with `ANALYSIS_PROVIDER=mock`; it is not used
as a fallback when a real provider fails.

------------------------------------------------------------------------

## Phase 6 --- Real LLM Integration

Implement:

-   provider client
-   structured output
-   prompt
-   response validation
-   timeout
-   failure handling
-   environment-based secrets

Keep provider-specific code isolated.

Implementation progress: OpenAI Chat Completions integration is implemented
with strict structured JSON output, environment-based API key/model settings,
request timeouts, output validation, and explicit provider error responses.
OpenAI is selected by default; set `ANALYSIS_PROVIDER=mock` for offline use.
Analysis sends the selected resume text and job description to OpenAI.
Automated tests use a mocked HTTP server; a live provider call still requires
a locally configured OpenAI API key.

------------------------------------------------------------------------

## Phase 7 --- React Foundation

Implement:

-   React app
-   routing
-   API client
-   auth state
-   login
-   registration
-   protected routes
-   layout

------------------------------------------------------------------------

## Phase 8 --- Resume UI

Implement:

-   upload
-   resume list
-   details
-   errors
-   loading states

------------------------------------------------------------------------

## Phase 9 --- Job UI

Implement:

-   create
-   list
-   details
-   update
-   delete
-   status

------------------------------------------------------------------------

## Phase 10 --- Analysis UI

Implement:

-   resume selection
-   job selection
-   analysis action
-   loading
-   score
-   skills
-   missing keywords
-   recommendations
-   history

------------------------------------------------------------------------

## Phase 11 --- Application Tracking

Implement:

-   save application
-   status
-   notes
-   dashboard
-   filtering

------------------------------------------------------------------------

## Phase 12 --- Redis

Implement:

-   rate limiting
-   selected cache
-   invalidation
-   configuration
-   tests

------------------------------------------------------------------------

## Phase 13 --- Docker & Deployment

Implement:

-   Dockerfiles
-   Compose
-   environment documentation
-   production builds
-   deployment documentation

------------------------------------------------------------------------

# 30. Definition of Done

A feature is complete only when:

1.  Code is implemented.
2.  Relevant tests exist.
3.  Tests pass.
4.  API/UI behavior is verified.
5.  Security/ownership is checked.
6.  Error handling exists.
7.  Documentation is updated.
8.  Git diff has been reviewed.

Do not claim completion just because the project compiles.

------------------------------------------------------------------------

# 31. Codex Prompts

## Phase 0

``` text
Read MD Files/AGENTS.md and MD Files/ARCHITECTURE.md.

We are building the AI Job Application & Resume Analyzer.

Implement Phase 0 only.

Inspect the repository and prepare the project structure and documentation. Do not implement business features.

At the end:
1. Summarize what you created.
2. Verify the repository.
3. Update documentation if required.
4. Give me the exact next prompt for Phase 1.
```

## Phase 1

``` text
Read MD Files/AGENTS.md and the Phase 1 section of MD Files/ARCHITECTURE.md.

Implement Phase 1 only: Backend Foundation.

Create the Spring Boot + Maven backend using Java 17 and MySQL configuration, clean package structure, validation, global exception handling, logging, and GET /api/v1/health.

Do not implement authentication, resume management, jobs, LLM integration, Redis, or frontend.

Run relevant tests and verify the application.

At the end:
- summarize changes
- list files changed
- report tests
- report verification
- update documentation
- provide the next exact prompt
```

## Generic next-phase prompt

``` text
Read MD Files/AGENTS.md and the relevant phase in MD Files/ARCHITECTURE.md.

Inspect the existing implementation first.

Implement ONLY the next incomplete phase.

Before coding:
1. Identify what is already implemented.
2. Identify dependencies.
3. Give a short plan.

While coding:
- follow the documented architecture
- reuse existing code
- avoid unnecessary dependencies
- do not implement future phases
- maintain security and ownership checks

After coding:
1. Run relevant tests.
2. Fix failures caused by the changes.
3. Verify behavior.
4. Update documentation/progress.
5. Summarize files changed.
6. Explain important design decisions.
7. State remaining work.
8. Provide the next exact Codex prompt.
```

------------------------------------------------------------------------

# 32. Interview-Level Expectations

The final project should allow the developer to confidently explain:

-   Why Spring Boot?
-   Why MySQL?
-   Why JPA/Hibernate?
-   Why DTOs?
-   Why JWT?
-   How is authorization enforced?
-   How are resumes stored and parsed?
-   How is the match score calculated?
-   Why use deterministic scoring plus an LLM?
-   How is LLM output validated?
-   Why abstract the LLM provider?
-   Why Redis?
-   Why rate limit analysis?
-   How would asynchronous analysis work?
-   Why start with a modular monolith?
-   How would you scale the application?
-   How would you handle LLM failures?
-   How would you prevent users from accessing another user's data?

The code and documentation should make these answers visible rather than
relying on undocumented assumptions.

------------------------------------------------------------------------

# 33. Long-Term Architecture

Do not begin with microservices.

Initial architecture:

``` text
React
  |
Spring Boot
  |
+---------+---------+
|         |         |
MySQL Redis        LLM
```

Future architecture, only if justified:

``` text
                 React
                   |
              API Gateway
                   |
        +----------+----------+
        |          |          |
       Auth      Resume      Job
      Service    Service    Service
        |          |          |
        +----------+----------+
                   |
            Analysis Service
                   |
                 Queue
                   |
                Worker
                   |
                  LLM
```

The modular monolith should keep module boundaries clean enough to make
future extraction possible.

------------------------------------------------------------------------

# 34. Final Rule

Build incrementally.

Do not generate the entire application in one step.

For every phase:

``` text
Understand
   ->
Plan
   ->
Implement
   ->
Test
   ->
Verify
   ->
Document
   ->
Move to next phase
```

The architecture document is the source of truth for system design.
`AGENTS.md` is the concise instruction layer that tells Codex how to use
this document.
