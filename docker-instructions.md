# Rewardo API Docker Instructions

## Dockerfile Overview

The Dockerfile for Rewardo API:
- Uses Eclipse Temurin's Alpine-based JRE 24 as the base image (smallest available JDK 24 image)
- Copies the rewardo-api.jar from the local filesystem
- Runs the application with the postgres profile

## Usage Instructions

### Prerequisites
- Docker installed on your system
- The rewardo-api.jar file built and available in the same directory as the Dockerfile

### Building the Docker Image

```bash
docker build -t rewardo-api:latest .
```

### Running the Container

```bash
docker run -p 8080:8080 --name rewardo-api rewardo-api:latest
```

### Running with PostgreSQL

The application is configured to connect to a PostgreSQL database at hostname "postgres". You can use Docker Compose or run a PostgreSQL container with the appropriate network configuration:

```bash
# Create a network
docker network create rewardo-network

# Run PostgreSQL
docker run --name postgres --network rewardo-network -e POSTGRES_DB=rewardo -e POSTGRES_USER=rewardo -e POSTGRES_PASSWORD=rewardo -d postgres:latest

# Run Rewardo API
docker run -p 8080:8080 --name rewardo-api --network rewardo-network rewardo-api:latest
```

## Environment Variables

You can override the default database connection settings by passing environment variables:

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://custom-postgres-host:5432/custom-db \
  -e SPRING_DATASOURCE_USERNAME=custom-user \
  -e SPRING_DATASOURCE_PASSWORD=custom-password \
  --name rewardo-api \
  rewardo-api:latest
```