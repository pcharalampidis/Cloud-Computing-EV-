# AI Prompting Guide

Use these prompts step-by-step when generating the implementation.

The project should stay simple and suitable for a student coursework submission:
* Java Jersey JAX-RS
* raw JDBC
* PostgreSQL
* HTML/CSS/Vanilla JS
* Leaflet.js
* Heroku deployment
* no Java server sessions
* no complex frameworks unless necessary

---

## Step 1: Database Setup

Prompt:

"Based on the Database Schema, generate a PostgreSQL `schema.sql` file and a pure JDBC `DatabaseManager.java` class.

Requirements:
1. Use PostgreSQL.
2. Read database configuration from environment variables using `System.getenv()`.
3. Support Heroku-style variables such as `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, and `JDBC_DATABASE_PASSWORD`.
4. Create tables for:
   - users
   - auth_tokens
   - stations
   - connectors
   - bookings
5. Add foreign keys.
6. Add CHECK constraints:
   - user role must be `DRIVER` or `ADMIN`
   - booking status must be `ACTIVE` or `CANCELLED`
   - `end_time > start_time`
7. Insert simple mock data for:
   - one admin
   - two drivers
   - a few stations
   - connectors
   - sample bookings
8. Do not store plaintext passwords. Use password hashes in the sample data."

---

## Step 2: DAOs and Concurrency

Prompt:

"Create Java DAO classes using raw JDBC.

Create:
- UserDAO
- TokenDAO
- StationDAO
- ConnectorDAO
- BookingDAO

For `BookingDAO.createBooking()` and `BookingDAO.updateBooking()`, use a database transaction.

Important concurrency requirements:
1. Do NOT use `SELECT COUNT(*) ... FOR UPDATE`.
2. Start a transaction by setting auto-commit to false.
3. Lock the relevant user row using:

```sql
SELECT username FROM users WHERE username = ? FOR UPDATE;
```

4. Lock the relevant connector row using:

```sql
SELECT connector_id, station_id FROM connectors WHERE connector_id = ? FOR UPDATE;
```

5. Check that `end_time > start_time`.
6. Check there is no overlapping ACTIVE booking for the same connector.
7. Check the driver has no overlapping ACTIVE booking.
8. Insert or update the booking.
9. Commit if successful.
10. Roll back if an exception or overlap occurs.
11. Throw a custom `BookingConflictException` if there is an overlap.

Use this overlap condition:

```sql
start_time < ? AND end_time > ?
```

Where the parameters are requested end time and requested start time."

---

## Step 3: Availability Logic

Prompt:

"Create availability logic for `GET /api/connectors/{id}/availability?date=YYYY-MM-DD`.

Do not create an `available_slots` table.

Instead:
1. Use fixed opening hours, for example 08:00 to 21:00.
2. Generate one-hour slots dynamically.
3. Query ACTIVE bookings for the selected connector and date.
4. Remove generated slots that overlap existing ACTIVE bookings.
5. Return the remaining slots as JSON."

---

## Step 4: JAX-RS REST API

Prompt:

"Using Jersey JAX-RS, create REST resource/controller classes based on the API Design document.

Create:
- AuthResource
- StationResource
- ConnectorResource
- AvailabilityResource
- BookingResource

Use annotations such as:
- `@Path`
- `@GET`
- `@POST`
- `@PUT`
- `@DELETE`
- `@Produces(MediaType.APPLICATION_JSON)`
- `@Consumes(MediaType.APPLICATION_JSON)`

Implement these endpoints:
- `POST /api/auth/login`
- `GET /api/stations`
- `GET /api/stations/{id}`
- `POST /api/stations`
- `PUT /api/stations/{id}`
- `DELETE /api/stations/{id}`
- `POST /api/stations/{id}/connectors`
- `PUT /api/connectors/{id}`
- `DELETE /api/connectors/{id}`
- `GET /api/connectors/{id}/availability?date=YYYY-MM-DD`
- `GET /api/bookings`
- `POST /api/bookings`
- `PUT /api/bookings/{id}`
- `DELETE /api/bookings/{id}`

Return suitable HTTP status codes:
- 200 OK
- 201 Created
- 204 No Content
- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict"

---

## Step 5: Filters: Security and Logging

Prompt:

"Create two Jersey filters.

1. `AuthFilter` implementing `ContainerRequestFilter`.

Requirements:
- Read the `Authorization` header.
- Expect `Bearer <token>`.
- Validate the token using the `auth_tokens` table.
- Attach the authenticated username and role to the request context.
- Enforce RBAC:
  - DRIVER can manage only own bookings.
  - ADMIN can manage stations, connectors, and all bookings.
- Return `401 Unauthorized` for missing/invalid tokens.
- Return `403 Forbidden` for insufficient role.

2. `LoggingFilter` implementing both `ContainerRequestFilter` and `ContainerResponseFilter`.

Log:
- timestamp
- HTTP method
- URI
- response status code
- processing time
- Heroku instance identifier using `System.getenv("DYNO")`."

---

## Step 6: Frontend

Prompt:

"Create a frontend using plain HTML, CSS, and Vanilla JavaScript.

Requirements:
1. Login form.
2. Store the returned auth token in browser storage.
3. Use `fetch()` to call REST endpoints.
4. Send the token in the `Authorization` header.
5. Use Leaflet.js to display charging stations on a map.
6. Allow users to:
   - view stations
   - inspect station connectors
   - select a date
   - view available slots
   - create bookings
   - view bookings
   - modify future bookings
   - cancel future bookings
7. If the logged-in user is ADMIN, show extra UI for:
   - adding/updating/deleting stations
   - adding/updating/deleting connectors
8. If the logged-in user is DRIVER, hide admin controls."

---

## Step 7: Heroku Deployment

Prompt:

"Generate the files needed to deploy this Java web application to Heroku.

Include:
1. `pom.xml` configured for a Jersey/JAX-RS Java web app.
2. A `Procfile`.
3. Instructions for using Heroku Postgres.
4. Instructions for setting environment/config variables.
5. Ensure the app reads database configuration from `System.getenv()`.
6. Ensure no database credentials are hardcoded."

---

## Step 8: Test Cases

Prompt:

"Create a simple manual and automated test plan for the EV charging booking system.

Include tests for:
1. Driver login succeeds with valid credentials.
2. Login fails with invalid credentials.
3. Driver cannot see another driver's bookings.
4. Admin can see all bookings.
5. Driver can create a valid booking.
6. Double booking the same connector/time returns `409 Conflict`.
7. Same driver overlapping booking returns `409 Conflict`.
8. Booking with `end_time <= start_time` returns `400 Bad Request`.
9. Cancelling a past or already-started booking is blocked.
10. Driver cannot manage stations/connectors.
11. Admin can create, update, and delete stations.
12. Admin can create, update, and delete connectors.
13. App reads database configuration from environment variables.
14. Logging filter records method, URI, status, processing time, timestamp, and dyno ID."