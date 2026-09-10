# C-Star

Company-wide employee appreciation platform. Mono repo containing backend services.

## Modules

| Module | Description | Tech |
|---|---|---|
| `c-star-core-service` | Appreciation main path: send/list/ranking/quota/SSO lazy sync | Micronaut + Java 21 + MariaDB + gRPC |
| `c-star-api-gateway` | SSO JWT verify + REST↔gRPC routing + protocol conversion | Micronaut + Java 21 + gRPC |

## Tech Stack

- Language: Java 21
- Framework: Micronaut 4.4.x
- Data access: Micronaut Data JDBC
- Database: MariaDB (per-service schema)
- Inter-service: gRPC (protobuf contract)
- Migration: Flyway
- Build: Gradle (Kotlin DSL), multi-module
- Deployment: Kubernetes + ArgoCD GitOps

## Build

```bash
./gradlew build          # build all modules
./gradlew test           # run all tests
./gradlew check          # styleCheck + tests + coverage
```

## Docs

Design docs (PRD / HLD / Detail Design / ADR / System Design / Glossary) live in the `ai-platform` workbench at `c-star/`, not in this repo.
