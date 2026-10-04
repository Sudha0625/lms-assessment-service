# assessment-service (LearningHub · Phase 1)

Owner: Sai Sudha Piratla · Port 8106 · Database `assessmentdb` · Base path `/api/assessment`

## Stack
Spring Boot 3.5 · Java 21 · Spring Data JPA · Flyway · PostgreSQL 16 · Spring Cloud Config client

## Layers
`web` (controllers) → `service` (business rules) → `repo` (Spring Data) → `entity` (JPA) → PostgreSQL

Bounded context: assessments, questions, attempts (quiz attempts and assignment submissions), grading.
Other contexts are referenced only by id (`teacher_username`, `username`, `term_id`), never by foreign key.

## Identity in Phase 1 (no auth yet)
Send headers `X-Username` and `X-Role` (`student` | `teacher` | `admin`). Defaults: `student1` / `student`.
Phase 2 replaces `CurrentUserResolver` with JWT claims.

## Calls to other services (fail-soft)
- enrollment-service `GET /api/enrollment/my-classes` filters a student's list to enrolled subjects.
- user-service `GET /api/users/teachers/{username}` supplies the teacher display name.
If either is down, the service logs a warning and carries on (it stays independently operable).

## Profiles (config)
`config-repo/assessment-service-dev.yml` and `-prod.yml` are what the config server serves.
Copies are bundled as `application-dev.yml` / `application-prod.yml` as a fallback.

| | dev | prod |
|---|---|---|
| DB host | localhost | `${DB_HOST}` (docker network) |
| Credentials | postgres/postgres | env `DB_USER` / `DB_PASSWORD` |
| SQL logging | on | off |
| Log level | DEBUG | INFO |

## Run
**Docker**
```
docker compose --profile dev  up --build
docker compose --profile prod up --build     # stop dev first (same port)
```
**Local**: start Postgres (`docker compose up postgres`), then `mvn spring-boot:run`.

Check: `curl localhost:8106/actuator/health` and `curl localhost:8106/api/assessment/list`

## Postman
Import `postman/assessment-service.postman_collection.json`, then run the whole collection with the Runner (folders 0 to 4, in order).
Every request carries its own `X-Username` / `X-Role` headers and a status-code check. To re-run from a clean state: `docker compose down -v && docker compose --profile dev up --build`.

## Seed data
Flyway `V2__seed.sql`: 2 terms, 4 assessments (3 quizzes, 1 assignment), 9 questions, 2 attempts for `student1`.
