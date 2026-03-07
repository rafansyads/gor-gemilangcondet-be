# Sistem Informasi GOR Gemilang Condet Backend API

## Java Springboot Directory Structure

This repository will also contains the Springboot directory in `src/main/java/io/mpruy/gor_gemilangcondet/backend_api`.  Below is the directory structure:

```
src/
└── main/
├── java/
│ └── io/mpruy/gor_gemilangcondet/backend_api/
│   ├── Application.java # Spring Boot entrypoint
│   ├── controller/ # REST controllers / endpoints
│   ├── service/ # Business logic / services
│   ├── repository/ # Spring Data JPA repositories
│   ├── model/ # JPA entities / domain models
│   ├── dto/ # Data Transfer Objects
│   ├── config/ # Configuration classes (Beans, CORS, etc.)
│   ├── security/ # Security config (JWT, filters)
│   ├── exception/ # Custom exceptions & handlers
│   └── util/ # Utilities and helpers
└── resources/
  ├── application.yml or application.properties
  ├── static/ # Static assets (if any)
  └── templates/ # Server-side templates (if used)
test/
└── java/ # Unit & integration tests mirror package structure
```

### Java Springboot Directory Structure

- **controller**: API endpoints and request mappings.
- **service**: Core business logic and orchestration.
- **repository**: Database access layer (Spring Data).
- **model**: Entity definitions and domain objects.
- **config/security/exception**: App configuration, security rules, and error handling.
- **resources**: App properties, static assets, and templates.
- **tests**: Unit and integration tests.

## Task Owners (to be added and adapted later on)

- Rafansya Daryltama Santoso (2306211231): `login`, `registrasi`, `logout`, `transaksi kantin dan toko`, `manajemen membership`

## Getting Started

1. Clone this repository using `git clone` with HTTPS URL: `https://gitlab.cs.ui.ac.id/propensi-2025-2026-genap/kelas-c/mpruy/mpruy-backend.git`;
2. To test the `main` or others branches, kindly switch branch in terminal with `git checkout` to desired branch (i.e. `git checkout main`);
3. Setup PostgreSQL Docker first with given `docker-compose.dev.yml` using this command: `docker-compose -f docker-compose.dev.yml up -d`. **(Best Practice):** This step can also be done with using both `docker-compose.yml`(make another one) and `docker-compose.dev.yml` using `docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d` (can also be done with docker-compose.prod.yml with similar command);
4. After making sure the docker container is up, run `./gradlew bootRun` or `.\gradlew.bat bootRun` and the application is ready to run.