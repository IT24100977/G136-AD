# Ride Management Service: RideLink

Ride requests, driver assignment and the ride lifecycle. This is the integrating service: it calls the Driver & Vehicle Service and the Fare & Payment Service over synchronous REST.

**Owner:** Member 3 (fill in). **Port:** 8082. **Database:** PostgreSQL `ride_db` (owned only by this service).

## Prerequisites
JDK 21+, Maven, PostgreSQL (`CREATE DATABASE ride_db;`). Driver service (8082) and Fare service (8084) running for the full workflow.

## Configuration (see `.env.example`; never commit real values)
| Variable | Purpose                               |
|---|---------------------------------------|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL                            |
| `JWT_SECRET` | Identical to the other three services |
| `DRIVER_SERVICE_URL` | default `http://localhost:8081`       |
| `FARE_SERVICE_URL` | default `http://localhost:8083`       |

## Run / test
```bash
mvn spring-boot:run
mvn test
```
Swagger UI: http://localhost:8082/swagger-ui.html

## Endpoints
| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/rides` | PASSENGER | Request a ride: fare estimate, nearest eligible driver, assignment |
| GET | `/rides/mine` | Authenticated | Own rides (ADMIN: all) |
| GET | `/rides/{id}` | Passenger, driver or ADMIN | Ride details |
| GET | `/rides/{id}/history` | Passenger, driver or ADMIN | Status history |
| PATCH | `/rides/{id}/accept` | Assigned DRIVER | ASSIGNED to ACCEPTED |
| PATCH | `/rides/{id}/start` | Assigned DRIVER | ACCEPTED to IN_PROGRESS |
| PATCH | `/rides/{id}/complete` | Assigned DRIVER | IN_PROGRESS to COMPLETED, final fare, payment |
| PATCH | `/rides/{id}/cancel` | Passenger, driver or ADMIN | Before the trip starts |

## Lifecycle
REQUESTED, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED. CANCELLED is allowed from REQUESTED, ASSIGNED and ACCEPTED. COMPLETED and CANCELLED are final.

## Assignment rule (documented)
Ask the Driver service for eligible drivers near the pickup (nearest first), pick the first, and mark it ON_TRIP so it cannot be double-booked. Released back to AVAILABLE on completion or cancellation.

## Interservice communication
| Interaction | Style | Why |
|---|---|---|
| Ride to Driver: eligible drivers, set availability | Synchronous REST | The answer is needed immediately to decide the assignment |
| Ride to Fare: estimate, final fare | Synchronous REST | The passenger and the completion response need the amount now |
| Ride to Fare: record payment | Synchronous REST | Result stored on the ride; failure is recorded as `paymentStatus=FAILED`, the ride stays COMPLETED |

Outbound calls use a short-lived service token (role ADMIN, subject `ride-service`) signed with the shared secret, plus connect/read timeouts. Remote failures return 503 (unavailable) or 502 (rejected).

## Negative scenarios
No available driver: 409. Invalid transition: 409. Wrong driver or non-participant: 403. Unknown ride: 404. Invalid input: 400. Missing/invalid token: 401. Driver or Fare service down: 503.
