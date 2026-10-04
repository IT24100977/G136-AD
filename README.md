# Driver & Vehicle Service: RideLink

Manages the driver's operational profile, vehicle details, availability, service area and simulated current location, and answers the "which drivers are eligible for this pickup?" query used by Ride Management.

| |                                                                                                                        |
|---|------------------------------------------------------------------------------------------------------------------------|
| **Primary owner** | [Member 2 name]                                                                                                        |
| **Port** | 8081                                                                                                                   |
| **Database** | MongoDB `driver_db` (collection `driver_profiles`), private to this service                                            |
| **Tech** | Java 21, Spring Boot 4.1.1, Spring Web, Spring Data MongoDB, Spring Security (JWT), Bean Validation, springdoc-openapi |

## Role in the system

- Validates JWTs locally with the shared secret. It never calls the Account Service.
- A driver's id **is** their account id (the JWT `sub`). It is a reference to the Account Service, not a foreign key.
- It is called by the **Ride Management Service** (eligible drivers, set `ON_TRIP` / `AVAILABLE`) and by drivers themselves. It calls no other service.

## Prerequisites

- JDK 21 or newer, Maven
- MongoDB running locally, for example `docker run -d -p 27017:27017 --name mongo mongo:7`

`driver_db` is created automatically on the first write.

## Configuration

Copy `.env.example` to `.env`, or set the same variables in your IDE run configuration. **Never commit real values.**

| Variable | Purpose | Example |
|---|---|---|
| `MONGO_URI` | Mongo connection string | `mongodb://localhost:27017/driver_db` |
| `JWT_SECRET` | Shared signing secret, **identical in all four services**. No default, so the service will not start without it. | (long random string agreed by the group) |

Business settings in `application.properties`: `app.driver.default-radius-km=5` and `app.driver.max-location-age-minutes=30`.

The port is `server.port=8082`. If your group uses different ports, change this one line and make sure the Ride service's `DRIVER_SERVICE_URL` points to it.

## Run and test

```bash
mvn spring-boot:run     # start the service
mvn test                # 9 unit tests, no database needed
```

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

In Swagger, log in through the Account Service, click **Authorize**, and paste the JWT.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/drivers` | DRIVER | Create the caller's own profile (id taken from the JWT) |
| GET | `/drivers/{id}` | Authenticated | View a driver |
| PUT | `/drivers/{id}` | Owner DRIVER or ADMIN | Update licence number and service area |
| PUT | `/drivers/{id}/vehicle` | Owner DRIVER | Register or replace the vehicle |
| PATCH | `/drivers/{id}/availability` | Owner DRIVER or ADMIN | `OFFLINE`, `AVAILABLE` or `ON_TRIP` |
| PUT | `/drivers/{id}/location` | Owner DRIVER | Set simulated latitude and longitude |
| GET | `/drivers/eligible?lat&lng&radiusKm&vehicleType&serviceArea&limit` | Authenticated | Nearest eligible drivers |

"Owner" means the `{id}` in the path must equal the caller's account id. ADMIN is also used by the Ride service's short-lived service token when it reserves or releases a driver.

## Request examples

Send each body as `Content-Type: application/json`.

**`POST /drivers`**

```json
{
  "licenseNumber": "LIC-100001",
  "serviceArea": "Colombo"
}
```

**`PUT /drivers/{id}/vehicle`**

```json
{
  "plateNumber": "WP-CAB-1234",
  "make": "Toyota",
  "model": "Prius",
  "color": "White",
  "seats": 4,
  "type": "CAR"
}
```

**`PUT /drivers/{id}/location`**

```json
{
  "latitude": 6.9275,
  "longitude": 79.8620
}
```

**`PATCH /drivers/{id}/availability`**

```json
{
  "status": "AVAILABLE"
}
```

Vehicle types: `BIKE`, `TUK`, `CAR`, `VAN`.

## Data model

One document per driver, with the vehicle embedded:

```json
{
  "_id": "9de3f560-becd-4a64-a90d-7a094d2f70b2",
  "licenseNumber": "LIC-100001",
  "serviceArea": "Colombo",
  "status": "AVAILABLE",
  "latitude": 6.9275,
  "longitude": 79.8620,
  "locationUpdatedAt": "2026-10-04T05:50:00Z",
  "vehicle": { "plateNumber": "WP-CAB-1234", "make": "Toyota", "model": "Prius", "color": "White", "seats": 4, "type": "CAR" }
}
```

Unique indexes: `licenseNumber`, and `vehicle.plateNumber` (sparse, so drivers without a vehicle do not collide).

**Why MongoDB:** a driver and vehicle are one natural document, and availability and location change often and are read together. PostgreSQL was considered; it would also work but adds no benefit for this access pattern.

## Eligibility rule (documented)

A driver is returned by `/drivers/eligible` only if all of these hold:

1. status is `AVAILABLE`
2. a vehicle is registered
3. the location was updated within the last 30 minutes
4. the straight-line (Haversine) distance to the pickup is within `radiusKm` (default 5)
5. (optional) the vehicle type and service area match the filters

Results are sorted nearest first and capped by `limit` (default 5, maximum 20). A driver can only go `AVAILABLE` if a vehicle and a location exist.

## Security

- JWT bearer authentication; the role claim is `PASSENGER`, `DRIVER` or `ADMIN`.
- Method-level rules with `@PreAuthorize`, including ownership checks (`#id == authentication.name`).
- No secrets in the repository; configuration comes from environment variables.
- Errors share one JSON shape: `timestamp`, `status`, `error`, `message`, `path`.

## Negative scenarios

| Scenario | Result |
|---|---|
| No or invalid token | 401 |
| Passenger creates a driver profile, or changes a driver's vehicle or availability | 403 |
| Driver edits another driver's data | 403 |
| Duplicate profile, licence number or plate number | 409 |
| Going `AVAILABLE` without a vehicle or a location | 409 |
| Unknown driver id | 404 |
| Invalid input (blank licence number, seats outside 1 to 12, latitude outside -90..90) | 400 |
| Invalid eligible-drivers coordinates or radius | 400 |

## Troubleshooting

- **Service will not start, "Could not resolve placeholder JWT_SECRET":** the variable is not set. Set it in the run configuration or `.env`.
- **Cannot connect to MongoDB:** check MongoDB is running on the URI in `MONGO_URI`.
- **Eligible list is empty:** the driver must be `AVAILABLE`, have a vehicle, and have set a location in the last 30 minutes. Repeat the location call.
- **401 from the Ride service's calls:** the JWT secret differs between services.
