# be-interview-prep

Five Spring Boot features built as one application, each shipped as its own pull request.

## Prerequisites

- Java 21 (Spring Boot 3.5 needs Java 17 or newer)
- Docker (optional)

No database to install: the app uses an in-memory H2 database.

## Run

```bash
./mvnw spring-boot:run
```

Or with Docker:

```bash
docker build -t be-interview-prep .
docker run -p 8080:8080 be-interview-prep
```

Health check: `curl http://localhost:8080/actuator/health`

## Test

```bash
./mvnw verify
```

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

Video:
