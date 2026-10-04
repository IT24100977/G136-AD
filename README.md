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
