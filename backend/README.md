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

Run tests with:

```powershell
mvn test
```

## Authentication endpoints

- `POST /api/v1/auth/register` — accepts `fullName`, `email`, and `password`; returns a bearer token and user profile.
- `POST /api/v1/auth/login` — accepts `email` and `password`; returns a bearer token and user profile.
- `GET /api/v1/auth/me` — requires `Authorization: Bearer <accessToken>`.

Passwords must be at least 8 characters and no more than 72 UTF-8 bytes. The
token lifetime defaults to one hour and can be changed with
`JWT_EXPIRATION_MS`.
