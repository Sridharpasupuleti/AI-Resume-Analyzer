# AI Job Application & Resume Analyzer --- Codex Instructions

## Project

Build a production-style full-stack AI Job Application & Resume
Analyzer.

Stack:

-   Java
-   Spring Boot
-   Spring Security
-   Spring Data JPA / Hibernate
-   MySQL
-   Redis
-   React.js
-   REST APIs
-   LLM integration
-   Maven
-   Git/GitHub
-   Docker later

## Source of truth

The detailed architecture and phased implementation plan is in:

`MD Files/ARCHITECTURE.md`

Use that document for architecture, modules, API design, data model,
development phases, and acceptance criteria.

Do not implement the entire project in one turn.

## Working rules

1.  Inspect the current repository before modifying it.
2.  Determine which development phase is actually incomplete.
3.  Implement only the requested/current phase.
4.  Do not jump ahead to future phases.
5.  Keep controllers thin.
6.  Put business logic in services/application logic.
7.  Keep persistence logic in repositories.
8.  Use DTOs instead of exposing JPA entities from REST APIs.
9.  Validate external input.
10. Enforce authenticated-user ownership of resources.
11. Never hard-code passwords, API keys, JWT secrets, or other
    credentials.
12. Prefer small, testable classes.
13. Avoid unnecessary dependencies.
14. Write tests for meaningful business logic.
15. Run relevant tests after changes.
16. Update documentation when architecture or implementation status
    changes.
17. Do not claim a feature is complete without verification.

## LLM rules

The LLM is an external dependency.

-   Keep LLM integration behind an interface.
-   Provide a mock implementation for tests.
-   Require structured JSON output.
-   Validate LLM output before persistence.
-   Never allow the LLM to directly access the database.
-   Never put secrets in source code.
-   Handle timeout/failure cases explicitly.

## Architecture rules

Start as a modular monolith.

Do not introduce microservices, Kafka, RabbitMQ, Kubernetes, or other
distributed infrastructure unless the current task explicitly requires
it.

Redis should initially be used for concrete requirements such as rate
limiting or selected caching, not added everywhere.

## Documentation

Before a significant architectural change:

1.  Explain why the change is needed.
2.  Update `MD Files/ARCHITECTURE.md`.
3.  Keep the implementation consistent with the documentation.

## Completion report

After each implementation task, report:

-   What was implemented
-   Files changed
-   Tests executed
-   Verification performed
-   Any remaining issues
-   Next recommended phase

Read the relevant section of `MD Files/ARCHITECTURE.md` rather than loading
every project document for every small change.
