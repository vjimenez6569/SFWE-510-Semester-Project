# Phase 2 — Maven Docker Image Build

From the project root (Java 21, Maven, Docker Desktop running):

```powershell
mvn clean package dockerfile:build -DskipTests
cd docker
docker compose up -d --no-build
docker compose ps
```

The Maven command builds six executable Spring Boot JARs and six images tagged `ecommerce/<service>:0.0.1-SNAPSHOT`. The parent aggregator is skipped for image creation. Compose uses those same tags.

Alternatively, build images through Compose:

```powershell
mvn clean package -DskipTests
cd docker
docker compose up --build -d
```

Stop: `docker compose down`.

**Note:** A `clean` failure deleting `target/surefire-reports` is a Windows file lock, not a Docker plugin issue. Close IDEs or pause OneDrive syncing, or temporarily run `mvn package dockerfile:build -DskipTests`.

**Status:** Configuration was inspected and XML parsed, but a full Maven build and Docker deployment have not been executed here. Keycloak, service-to-service security, and runtime integration still require validation.
