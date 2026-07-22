# Phase 3: Backend Foundation

## Objective

Create the backend runtime foundation for the Adaptive API Gateway without implementing authentication, gateway routing behavior, Redis, Kafka, rate limiting, adaptive learning, anomaly detection, frontend, or WebSocket features.

## Runtime Stack

The backend foundation uses:

- Java 21 source compatibility.
- Maven with backend Maven wrapper scripts.
- Spring Boot 4.0.7.
- Spring Cloud Gateway Server WebFlux 5.0.x through the Spring Cloud 2025.1.2 release train.
- Spring Data JPA and JDBC.
- PostgreSQL driver.
- Flyway with the Phase 2 PostgreSQL migration.
- Spring Validation.
- Spring Actuator.
- Springdoc OpenAPI WebFlux UI.

These versions were selected from current official Spring and Maven Central metadata. Spring Cloud documents that the 2025.1.x release train supports Spring Boot 4.0.x and 4.1.x, and Spring Cloud Gateway 5.0.2 is the current stable Gateway line.

## Added Files

```text
backend/pom.xml
backend/mvnw
backend/mvnw.cmd
backend/.mvn/wrapper/maven-wrapper.properties
backend/src/main/java/com/adaptivegateway/AdaptiveGatewayApplication.java
backend/src/main/java/com/adaptivegateway/common/api/
backend/src/main/java/com/adaptivegateway/common/exception/
backend/src/main/java/com/adaptivegateway/common/pagination/
backend/src/main/java/com/adaptivegateway/common/web/
backend/src/main/java/com/adaptivegateway/config/
backend/src/main/java/com/adaptivegateway/foundation/
backend/src/main/resources/application.yml
backend/src/main/resources/application-local.yml
backend/src/main/resources/application-prod.yml
backend/src/test/java/com/adaptivegateway/AdaptiveGatewayApplicationTests.java
```

## Foundation Capabilities

The backend now has:

- A Spring Boot application entrypoint.
- Backend Maven wrapper pinned to Maven 3.9.16.
- PostgreSQL-only datasource configuration through environment variables.
- Flyway migration wiring for `backend/src/main/resources/db/migration`.
- Hibernate configured to validate the schema instead of generating tables.
- OpenAPI metadata and Swagger UI configuration.
- Actuator health, info, metrics, and Prometheus endpoint exposure.
- Correlation id propagation through `X-Correlation-Id`.
- Standard API success envelope.
- Standard API error envelope.
- Stable error code enum.
- Business exception type.
- Global exception handler.
- Shared pagination request DTO.
- A real system health endpoint at `GET /api/system/health`.

The health endpoint checks PostgreSQL through a real SQL query:

```sql
SELECT current_database(), current_schema(), version()
```

It does not use generated fixture data or process-local persistence.

## Configuration Contract

The backend requires these environment variables to start:

```text
SERVER_PORT
POSTGRES_JDBC_URL
POSTGRES_USERNAME
POSTGRES_PASSWORD
POSTGRES_POOL_MAX_SIZE
POSTGRES_POOL_MIN_IDLE
POSTGRES_CONNECTION_TIMEOUT_MS
ADAPTIVE_GATEWAY_SERVICE_NAME
ADAPTIVE_GATEWAY_API_VERSION
```

No embedded database profile is present.

## Phase 2 Migration Correction

The Phase 2 migration trigger creation statement was corrected during this phase so Flyway can create stable trigger names for every table.

The affected expression now builds the full trigger name first and passes it as one PostgreSQL identifier.

## Verification

Required verification for this phase:

- Maven dependency resolution: passed.
- Backend compilation: passed through `.\mvnw.cmd test`.
- Unit test execution: passed through `.\mvnw.cmd test`.
- Static scan for forbidden implementation markers: passed.
- Runtime startup against PostgreSQL: not executed in this environment because the required PostgreSQL environment variables are not set and no local PostgreSQL listener is available on port 5432.

Runtime startup depends on real PostgreSQL connectivity because this project intentionally does not include H2 or embedded database fallbacks.
