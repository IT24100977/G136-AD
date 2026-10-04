# Fare & Payment Service: RideLink

Fare estimation, final fare calculation, simulated payments and receipts.

**Owner:** Member 4 (fill in). **Port:** 8083. **Database:** PostgreSQL `fare_db` (owned only by this service).

## Prerequisites
JDK 21+, Maven, PostgreSQL (`CREATE DATABASE fare_db;`).

## Configuration (see `.env.example`; never commit real values)
| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL |
| `JWT_SECRET` | Identical to the other three services |
| `PAYMENT_MAX_AMOUNT` | Simulated card limit (default 50000). Lower it to demonstrate a declined payment |

Fare constants (`app.fare.*` in `application.properties`) are plain business settings, not secrets.

## Run / test
```bash
mvn spring-boot:run
mvn test
```
Swagger UI: http://localhost:8083/swagger-ui.html

## Fare rule (documented)
```
subtotal = baseFare + (perKm x distanceKm) + (perMinute x durationMinutes)
total    = max(minimumFare, subtotal x vehicleMultiplier)      (rounded to 2 decimals, LKR)
```
Base 100, per km 60, per minute 5, minimum 250. Multipliers: BIKE 0.6, TUK 0.8, CAR 1.0, VAN 1.4.
Worked example: 5 km, 15 min, CAR = 100 + 300 + 75 = **475.00**. The same trip by VAN = 475 x 1.4 = 665.00.
An **estimate** uses the straight-line (Haversine) distance and assumes 30 km/h for the duration. The **final fare** uses the distance and duration reported by the Ride service and is stored once per ride.

## Endpoints
| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/fares/estimate` | Authenticated | Estimate from pickup/destination coordinates |
| POST | `/fares/final` | ADMIN (service token) | Calculate and store the final fare for a ride (idempotent) |
| GET | `/fares/ride/{rideId}` | ADMIN | Stored final fare |
| POST | `/payments` | ADMIN (service token) | Record a simulated payment (201 new, 200 existing) |
| POST | `/payments/{id}/retry` | ADMIN | Retry a FAILED payment |
| GET | `/payments/{id}` | Passenger, driver or ADMIN | Payment status |
| GET | `/payments/ride/{rideId}` | Passenger, driver or ADMIN | Payment by ride |
| GET | `/payments/{id}/receipt` | Passenger, driver or ADMIN | Receipt (PAID payments only) |

## Contract used by the Ride Management Service
- `POST /fares/estimate` body: `pickupLatitude, pickupLongitude, destinationLatitude, destinationLongitude, vehicleType` returns `estimatedFare, currency, distanceKm, durationMinutes, ...`
- `POST /fares/final` body: `rideId, distanceKm, durationMinutes, vehicleType` returns `finalFare, currency, ...`
- `POST /payments` body: `rideId, passengerId, driverId, amount` returns `paymentId, status (PAID or FAILED), receiptNumber, ...`

## Simulated payment rules
A payment is stored per ride. It is **PAID** (with receipt number `RCPT-yyyyMMdd-XXXXXXXX`) unless the amount exceeds `PAYMENT_MAX_AMOUNT` or `simulateFailure=true` is sent, in which case it is **FAILED** with a reason and no receipt. The amount must equal the stored final fare, and a final fare must exist first.

## Negative scenarios
Payment before a final fare: 409. Amount not equal to the final fare: 400. Unknown vehicle type or invalid coordinates: 400. Receipt for a failed payment: 409. Retry of a payment that is not FAILED: 409. Stranger reading a payment: 403. Unknown payment or fare: 404. Missing/invalid token: 401.
# RideLink: Backend Microservices for a Ride-Sharing Platform

IT3130 Application Development, Group Assignment. Backend only: Swagger UI and the Postman collection are the official interfaces.

| **Release tag:** [v1.0.0]

## Service owners

| Service | Primary owner | Port | Database | Folder |
|---|---|------|---|---|
| Account Service | [Member 1 name] | 8081 | PostgreSQL `account_db` | `account-service/` |
| Driver & Vehicle Service | [Member 2 name] | 8081 | MongoDB `driver_db` | `driver-service/` |
| Ride Management Service | [Member 3 name] | 8082 | PostgreSQL `ride_db` | `ride-service/` |
| Fare & Payment Service | [Member 4 name] | 8083 | PostgreSQL `fare_db` | `fare-payment-service/` |

Each service owns its own database. No service reads or writes another service's data; they talk only through REST APIs.

## Architecture in one minute

- **Account** is the only service that issues JWTs. The other three validate the token locally with the same shared secret.
- **Ride Management** orchestrates the booking. It calls **Driver & Vehicle** (eligible drivers, driver availability) and **Fare & Payment** (estimate, final fare, simulated payment) over synchronous REST.
- Ride Management uses a short-lived service token (role ADMIN, subject `ride-service`) for those outbound calls.
- Details, diagrams and the communication comparison are in the technical report.

## Repository layout

```
ridelink/
  account-service/
  driver-service/
  ride-service/
  fare-payment-service/
  postman/                  exported collection and environment
  .github/workflows/        one CI workflow per service
  README.md                 this file
```

## Prerequisites

- JDK 21 or newer, Maven
- PostgreSQL (for Account, Ride and Fare)
- MongoDB (for Driver), for example `docker run -d -p 27017:27017 --name mongo mongo:7`
- Postman (optional, for the collection)

## Configuration

Every service has an `.env.example`. Copy it to `.env` or set the same variables in your IDE run configuration. **Never commit real values.**

| Variable | Used by | Meaning |
|---|---|---|
| `JWT_SECRET` | all four | Long random string. **Must be identical in all four services.** There is no default. |
| `JWT_EXPIRATION_MS` | Account | Token lifetime, default 86400000 (24 hours) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Account, Ride, Fare | PostgreSQL connection |
| `MONGO_URI` | Driver | MongoDB connection string |
| `DRIVER_SERVICE_URL` | Ride | default `http://localhost:8082` |
| `FARE_SERVICE_URL` | Ride | default `http://localhost:8084` |
| `PAYMENT_MAX_AMOUNT` | Fare | Simulated payment limit, default 50000 |
| `INCLUDE_ERROR_CAUSE` | Ride | Dev switch. Keep `false` for the demo. |

Create the PostgreSQL databases once:

```sql
CREATE DATABASE account_db;
CREATE DATABASE ride_db;
CREATE DATABASE fare_db;
```

MongoDB creates `driver_db` automatically on first write.

## Start-up order

1. PostgreSQL and MongoDB running
2. Account Service (8081)
3. Driver & Vehicle Service (8082)
4. Fare & Payment Service (8084)
5. Ride Management Service (8083)

In each service folder:

```bash
mvn spring-boot:run
```

## Swagger UI (OpenAPI documentation)

| Service | Swagger UI                            |
|---|---------------------------------------|
| Account | http://localhost:8080/swagger-ui.html |
| Driver & Vehicle | http://localhost:8081/swagger-ui.html |
| Ride Management | http://localhost:8082/swagger-ui.html |
| Fare & Payment | http://localhost:8083/swagger-ui.html |

OpenAPI JSON is at `/v3/api-docs` on each service. Use the **Authorize** button and paste a JWT from login.

## Sample test data (fictional)

| Role | Email | Password |
|---|---|---|
| Passenger | `passenger1@example.com` | `password123` |
| Passenger (second) | `passenger2@example.com` | `password123` |
| Driver | `driver1@example.com` | `password123` |
| Admin | `admin1@example.com` | `password123` |

Create them with `POST /accounts/register`, then log in with `POST /accounts/login`. [Describe here how the admin account is created if public ADMIN registration is disabled.]

Sample coordinates (Colombo): pickup Fort `6.9271, 79.8612`, destination Mount Lavinia `6.8389, 79.8653`, driver location `6.9275, 79.8620`.

## Running the tests

Unit tests need no database or running service. In each service folder:

```bash
mvn test
```

Integrated tests: import `postman/RideLink.postman_collection.json` and `postman/RideLink.postman_environment.json` into Postman, set the four service URLs in the environment, start all four services, and run the collection in order (setup, driver preparation, fare, ride lifecycle, negative scenarios).

## Main workflow (manual)

1. Register and log in as passenger and driver.
2. Driver: `POST /drivers`, `PUT /drivers/{id}/vehicle`, `PUT /drivers/{id}/location`, `PATCH /drivers/{id}/availability` with `AVAILABLE`.
3. Passenger: `POST /fares/estimate`, then `POST /rides`.
4. Driver: `PATCH /rides/{id}/accept`, `/start`, `/complete`.
5. Passenger: `GET /payments/ride/{rideId}` and `GET /payments/{id}/receipt`.

## Ride lifecycle

`REQUESTED` to `ASSIGNED` to `ACCEPTED` to `IN_PROGRESS` to `COMPLETED`. `CANCELLED` is allowed from `REQUESTED`, `ASSIGNED` and `ACCEPTED`. Invalid transitions return 409.

## Negative scenarios demonstrated

No available driver (409), invalid status transition (409), wrong role or non-participant (403), missing token (401), invalid input (400), unknown resource (404), failed simulated payment (ride stays COMPLETED with `paymentStatus` FAILED), Fare or Driver service unavailable (503).

## Version control workflow

- `main` always holds an integrated, demonstrable version and changes only through pull requests.
- `develop` is the integration branch.
- Work happens on feature branches named `feature/<service>-<topic>`, for example `feature/driver-eligible-query`.
- Every pull request needs at least one review from another member and a passing CI check.
- Commit messages are short and describe one change. The assessed version carries the release tag `[v1.0.0]`.

## Continuous integration

Each service has a GitHub Actions workflow in `.github/workflows/` that builds it and runs its tests (`mvn -B clean verify`) on pushes and pull requests to `main` and `develop`.

## Service READMEs

Each service folder contains its own README with endpoints, rules and negative scenarios:
[Account](account-service/README.md), [Driver & Vehicle](driver-service/README.md), [Ride Management](ride-service/README.md), [Fare & Payment](fare-payment-service/README.md).

## Known limitations

Documented in Section 9 of the technical report: no atomic driver reservation, no distributed transactions, synchronous payment recording without retries, and a shared symmetric JWT secret.
