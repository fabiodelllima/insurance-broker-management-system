# Ando IBMS API

Backend API for IBMS (Insurance Broker Management System).

## Stack

- Java 17
- Spring Boot 3.5.11
- Spring Data JPA + PostgreSQL
- Spring Data Redis
- Spring AMQP (RabbitMQ)
- Spring Security
- Spring Actuator

## Prerequisites

Infrastructure services running via `ando-ibms-infra`:

```
cd ../ando-ibms-infra/docker/compose
docker compose up -d
```

## Build & Run

```
./mvnw spring-boot:run
```

## Test

```
./mvnw test
```

## Branches

- `main` — production, protected
- `develop` — continuous integration
- `feature/*` — new features
