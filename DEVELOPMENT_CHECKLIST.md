# Development Checklist

## Inspection findings

- [x] Existing project inspected; preserving the current Maven project and package layout.
- [x] Java 17 source, Spring Security/JWT, JPA repositories/entities, Flyway migration, Docker configuration, README, Postman collection, unit tests, and MySQL Testcontainers tests inspected.
- [x] JDK 17 found and runnable at `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`; it is not on this shell's `PATH`.
- [ ] Maven executable not found on `PATH` or in the standard machine-wide install folders checked; `mvn clean verify` and `mvn spring-boot:run` were attempted and could not start.
- [ ] Docker CLI is present, but the Docker engine pipe is unavailable; `docker compose up --build` was attempted and could not reach the engine.
- [ ] `target/` artifacts exist but are not considered verification of the current source until a fresh build runs.

## Review and completion work

- [x] Access token expiration and JWT validation.
- [x] Hashed, one-time refresh-token rotation and logout revocation.
- [x] Account ownership checks and role-restricted admin routes.
- [x] Ordered pessimistic row locks, atomic transfer writes, idempotency constraint and request fingerprint.
- [x] Independent failed-transaction audit writes and rollback coverage.
- [x] MySQL/Flyway schema, decimal checks, required secret configuration, and non-root Docker image.
- [x] Add paged account statements, admin user search, and high-value transfer review heuristic.
- [x] Complete Postman examples for withdrawal/history/admin workflows.
- [x] Add authentication and transfer workflow Mermaid diagrams.
- [x] Add actuator and Docker application health checks, role provisioning notes, and interview discussion prompts.
- [x] Add statement opening/closing balance calculation and admin search/monitoring routes.
- [x] Add integration coverage for statements and admin operations.
- [ ] Fresh `mvn clean verify`, service startup, Flyway run, Testcontainers execution, and Docker startup once Maven and Docker engine are reachable.

## Verification record

- `pom.xml`: parsed as valid XML.
- `postman/Secure-Banking.postman_collection.json`: parsed as valid JSON.
- `docker compose config`: valid with required environment values supplied.
- Java compilation and tests: not yet run in this shell; `mvn` is unavailable.
- Container startup: not yet run; Docker Engine is unavailable.
