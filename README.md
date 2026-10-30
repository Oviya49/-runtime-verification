# Runtime Verification of Context Integrity Across Asynchronous Microservice Boundaries



**Author:** Oviya N  

**Institution:** Saveetha Engineering College, Chennai, India



## Overview



This project implements a runtime verification prototype to examine tenant-context integrity across asynchronous microservice communication.



In multi-tenant applications, tenant identity must remain correct as messages move between services and asynchronous workers. Missing, stale, or incorrectly applied tenant context can cause data-access operations to execute under the wrong tenant.



The prototype checks tenant context at data-access boundaries and records detected violations.



## Architecture



The prototype contains two Spring Boot services:



- **Service A – Producer:** Accepts requests, attaches tenant metadata, signs message fields using HMAC-SHA256, and publishes messages to RabbitMQ.

- **Service B – Consumer:** Receives messages asynchronously, verifies message signatures, checks tenant context during processing, and exposes verification metrics.



RabbitMQ acts as the message broker. The prototype uses an in-memory tenant-row store.



## Technology Stack



- Java 21

- Spring Boot 3.2.5

- RabbitMQ

- Maven

- Docker and Docker Compose

- HMAC-SHA256

- REST APIs



## Fault Scenarios



| ID | Fault scenario |

|---|---|

| F1 | Missing tenant context |

| F2 | Stale context reuse |

| F3 | Asynchronous context loss |

| F4 | Missing tenant filter |

| H1 | Forged message signature |

| H2 | Header/body tenant mismatch |



## Running the Project



### Prerequisites



Install:



- Java 21

- Docker Desktop

- Git



### Start the services



From the project root directory, run:



```powershell

docker compose -f docker/docker-compose.yml up --build

```



### Stop the services



```powershell

docker compose -f docker/docker-compose.yml down

```



## API Endpoints



### Producer service



**Base URL:** `http://localhost:8081`



Send a request:



```http

POST /api/request/send

Content-Type: application/json

```



Example request:



```json

{

 "tenantId": "TENANT_A",

 "fault": "NONE"

}

```



### Consumer service



**Base URL:** `http://localhost:8082`



Retrieve verification metrics:



```http

GET /api/metrics/all

```



## Experimental Evaluation



The evaluation compares:



- A runtime verifier with statement-level and row-level checks.

- A header-only baseline.

- Statement-only and row-only ablation configurations.



The experiments use controlled synthetic fault scenarios and a clean baseline. Detection performance is evaluated using true positives, false negatives, false positives, and true negatives.



The F2 stale-context scoring rule counts a violation only when the previous message processed by the same worker had a different tenant.



## Scope and Limitations



This is a research prototype evaluated under controlled conditions.



- Uses one consumer worker.

- Uses an in-memory tenant-row store rather than a production database.

- Uses synthetic fault injection.

- Runs in a single-host environment.

- Does not establish production-level security or guarantee tenant isolation in arbitrary deployments.



The HMAC signature verifies the integrity of signed fields under the configured key. It does not independently establish whether a producer is authorized to assign a tenant identity.



## Research Context



This implementation supports an experimental study of whether checking tenant context at data-access boundaries can detect context-integrity violations that header-level validation alone may miss.


