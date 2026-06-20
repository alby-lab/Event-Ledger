# Event Ledger — Distributed Microservices System

## Architecture Overview

Two independently deployable Spring Boot microservices connected via REST (OpenFeign):

```
Client
  │
  ▼
Event Gateway Service (:8080)   
  │  - POST /events                                                    
  │  - GET  /events/{eventId}                                         
  │  - GET  /events?account={id}                                       
  │  - GET  /health                                                     
  │
  H2 in-memory DB (eventgatewaydb)  
  
── communicated through [OpenFeign ] and Fallback mechanism handled by [Circuit Breaker] --

Account Service (:8081)
  
  - POST /accounts/{id}/transactions
   - GET  /accounts/{id}/balance
   - GET  /accounts/{id}
   - GET  /health
 
  H2 in-memory DB (accountservicedb)
```

Both services use **separate H2 in-memory databases** and share no state. Each runs independently.

## API Documentation

### Event Gateway Service (port 8080)

#### POST /events — Submit an event
```
POST http://localhost:8080/events
Content-Type: application/json

{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": { "source": "mainframe-batch", "batchId": "B-9042" }
}
```
- **201 Created** — new event stored and balance updated
- **200 OK** — duplicate `eventId` (idempotent, returns existing event)
- **400 Bad Request** — validation failure
- **503 Service Unavailable** — Account Service down

#### GET /events/{eventId} — Retrieve a single event
```
GET http://localhost:8080/events/evt-001
```
- **200 OK** — event found
- **404 Not Found** — unknown eventId

#### GET /events?account={accountId} — List events for account
```
GET http://localhost:8080/events?account=acct-123
```
- **200 OK** — list sorted by `eventTimestamp` ascending

#### GET /health
```json
{ "status": "UP", "service": "event-gateway-service" }
```

---

### Account Service (port 8081)

#### POST /accounts/{accountId}/transactions — Process a transaction
```
POST http://localhost:8081/accounts/acct-123/transactions
Content-Type: application/json

{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "currency": "USD"
}
```
- **201 Created** — balance updated
- Accounts are **auto-created** on first transaction
- Duplicate `eventId` is silently deduplicated (idempotent)

#### GET /accounts/{accountId}/balance
```json
{ "accountId": "acct-123", "balance": 150.00, "currency": "USD" }
```

#### GET /accounts/{accountId}
```json
{ "accountId": "acct-123", "currentBalance": 150.00, "currency": "USD", "createdAt": "..." }
```

#### GET /health
```json
{ "status": "UP", "service": "account-service" }
```

---

## Setup — Eclipse IDE

### Prerequisites
- Java 21+ (project compiles on Java 24)
- Maven 3.8+
- Eclipse IDE with M2Eclipse (Maven plugin)

### Import Projects
1. `File > Import > Maven > Existing Maven Projects`
2. Browse to `event-ledger/event-gateway-service` → Finish
3. Repeat for `event-ledger/account-service`

### Run in Eclipse
1. Run `AccountServiceApplication.java` as Java Application first (port 8081)
2. Run `EventGatewayApplication.java` as Java Application (port 8080)

---

## Test Execution

```bash
# Account Service tests (16 tests)
cd account-service
mvn clean test

# Event Gateway tests (21 tests)
cd event-gateway-service
mvn clean test
```

Test coverage:
- **Unit tests** — service-layer with mocked dependencies (EventServiceTest, AccountServiceTest)
- **Integration tests** — full Spring Boot context with MockMvc (EventControllerIntegrationTest, AccountControllerIntegrationTest)
- **Resiliency tests** — Account Service unavailability scenarios (ResiliencyTest)

---

## Docker Instructions

### Build and start everything
```bash
docker-compose up --build
```

### Verify services
```bash
curl http://localhost:8080/health
curl http://localhost:8081/health
```

### Submit a test event
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"eventId":"evt-001","accountId":"acct-123","type":"CREDIT","amount":150.00,"currency":"USD","eventTimestamp":"2026-05-15T14:02:11Z"}'
```

### Stop everything
```bash
docker-compose down
```

---

## Resiliency Strategy

**Fallback behaviour:**
- When circuit opens, `AccountServiceClientFallbackFactory` throws `AccountServiceUnavailableException`
- `GlobalExceptionHandler` maps this to **HTTP 503** (never a 500)
- GET endpoints do not call Account Service — they continue working normally

---

## Trace Propagation

**Example log line:**
```json
{"timestamp":"2026-05-15T14:02:11Z","level":"INFO","serviceName":"event-gateway-service",
 "traceId":"abc123","spanId":"def456","message":"Event stored: eventId=evt-001"}
```

---

## Metrics

Available at `GET /actuator/prometheus` and `GET /actuator/metrics`:
