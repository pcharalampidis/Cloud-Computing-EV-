# EV Charging Booking System - AI-Friendly Requirements Roadmap

## Purpose

This roadmap defines the recommended order for building the EV charging station booking system.

The project should stay simple and suitable for student coursework:

- Java Jersey JAX-RS REST API
- Raw JDBC
- PostgreSQL
- HTML, CSS, Vanilla JavaScript
- Leaflet.js map
- Heroku deployment
- Stateless token-based authentication
- No Hibernate
- No Spring Security
- No server-side Java sessions

---

# Phase 1: Confirm Core Requirements

## Goal

Before coding, confirm the main features required by the coursework.

## Requirements

The system must support:

1. User authentication.
2. Two roles:
   - DRIVER
   - ADMIN
3. Drivers can:
   - view charging stations
   - inspect connectors
   - view available slots
   - create bookings
   - view their own bookings
   - modify their own future bookings
   - cancel their own future bookings
4. Admins can:
   - view all bookings
   - manage any future booking
   - create, update, and delete stations
   - create, update, and delete connectors
5. The system must prevent:
   - double booking the same connector
   - one driver having overlapping bookings
   - modifying/cancelling bookings after the slot starts
6. The app must be deployable on Heroku.
7. The app must read database configuration from environment variables.
8. The app must log request details.

## Output

No code yet. Just confirm the scope.

---

# Phase 2: Database Design

## Goal

Create a simple PostgreSQL schema.

## Tables

Create these tables:

1. `users`
2. `auth_tokens`
3. `stations`
4. `connectors`
5. `bookings`

## Important Constraints

Add:

- primary keys
- foreign keys
- `CHECK (role IN ('DRIVER', 'ADMIN'))`
- `CHECK (status IN ('ACTIVE', 'CANCELLED'))`
- `CHECK (end_time > start_time)`

## Important Design Decision

Do not create an `available_slots` table.

Available slots are generated dynamically by the backend using fixed opening hours and existing bookings.

## Output Files

- `schema.sql`
- optional `mock_data.sql`

---

# Phase 3: Database Connection

## Goal

Create the JDBC database connection class.

## Requirements

Create `DatabaseManager.java`.

It must:

1. Use raw JDBC.
2. Connect to PostgreSQL.
3. Read database details from environment variables.
4. Not hardcode usernames, passwords, or URLs.

## Suggested Environment Variables

- `JDBC_DATABASE_URL`
- `JDBC_DATABASE_USERNAME`
- `JDBC_DATABASE_PASSWORD`

## Output Files

- `DatabaseManager.java`

---

# Phase 4: Java Model Classes

## Goal

Create simple Java model classes matching the database tables.

## Classes

Create:

- `User`
- `AuthToken`
- `Station`
- `Connector`
- `Booking`
- `TimeSlot`

## Output Files

- `User.java`
- `Station.java`
- `Connector.java`
- `Booking.java`
- `TimeSlot.java`

---

# Phase 5: DAO Layer

## Goal

Create DAO classes using raw JDBC.

## DAO Classes

Create:

- `UserDAO`
- `TokenDAO`
- `StationDAO`
- `ConnectorDAO`
- `BookingDAO`

## Requirements

DAOs should contain SQL logic only.

REST controllers should not contain raw SQL.

## Output Files

- `UserDAO.java`
- `TokenDAO.java`
- `StationDAO.java`
- `ConnectorDAO.java`
- `BookingDAO.java`

---

# Phase 6: Authentication Logic

## Goal

Allow users to log in and receive a token.

## Requirements

1. User sends username and password.
2. Server checks password against stored password hash.
3. If valid, server creates a random token.
4. Token is stored in `auth_tokens`.
5. Token is returned to the frontend.
6. Future requests send:

```http
Authorization: Bearer token-value
```

## Important

Do not use Java server sessions.

The API must be stateless.

## Output

- Login DAO logic
- Token creation logic
- Password hash verification

---

# Phase 7: Booking Concurrency Logic

## Goal

Implement the most important business rule safely.

The system must prevent double bookings even when two users book at the same time.

## Important Rule

Do not use:

```sql
SELECT COUNT(*) ... FOR UPDATE
```

## Correct Simple Transaction Plan

For `createBooking()` and `updateBooking()`:

1. Open database connection.
2. Set `autoCommit(false)`.
3. Lock the relevant user row:

```sql
SELECT username FROM users WHERE username = ? FOR UPDATE;
```

4. Lock the relevant connector row:

```sql
SELECT connector_id, station_id FROM connectors WHERE connector_id = ? FOR UPDATE;
```

5. Check `end_time > start_time`.
6. Check no overlapping ACTIVE booking exists for the same connector.
7. Check the driver has no overlapping ACTIVE booking.
8. Insert or update booking.
9. Commit.
10. Roll back if anything fails.

## Overlap Rule

Two bookings overlap if:

```sql
existing.start_time < requested_end_time
AND existing.end_time > requested_start_time
```

## Output

- `BookingDAO.createBooking()`
- `BookingDAO.updateBooking()`
- `BookingConflictException.java`

---

# Phase 8: Availability Logic

## Goal

Return available slots for a connector on a selected date.

## Design

No `available_slots` table is used.

The backend generates slots dynamically.

## Example Opening Hours

```text
08:00 to 21:00
```

Generate one-hour slots:

```text
08:00-09:00
09:00-10:00
10:00-11:00
...
20:00-21:00
```

Then remove slots that overlap existing ACTIVE bookings.

## Endpoint

```http
GET /api/connectors/{id}/availability?date=YYYY-MM-DD
```

## Output

- Availability method in DAO/service
- `TimeSlot` response objects

---

# Phase 9: REST API Resources

## Goal

Create the JAX-RS resource classes.

## Resource Classes

Create:

- `AuthResource`
- `StationResource`
- `ConnectorResource`
- `AvailabilityResource`
- `BookingResource`

## Endpoints

### Authentication

```http
POST /api/auth/login
```

### Stations

```http
GET /api/stations
GET /api/stations/{id}
POST /api/stations
PUT /api/stations/{id}
DELETE /api/stations/{id}
```

### Connectors

```http
POST /api/stations/{id}/connectors
PUT /api/connectors/{id}
DELETE /api/connectors/{id}
```

### Availability

```http
GET /api/connectors/{id}/availability?date=YYYY-MM-DD
```

### Bookings

```http
GET /api/bookings
POST /api/bookings
PUT /api/bookings/{id}
DELETE /api/bookings/{id}
```

## HTTP Status Codes

Use:

- `200 OK`
- `201 Created`
- `204 No Content`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

---

# Phase 10: Auth Filter and RBAC

## Goal

Protect the API and enforce roles.

## Create

`AuthFilter.java`

## Requirements

The filter must:

1. Read the `Authorization` header.
2. Validate the token from the database.
3. Identify the current username and role.
4. Allow or block requests based on role.
5. Return `401 Unauthorized` for missing/invalid token.
6. Return `403 Forbidden` for valid token but insufficient permissions.

## Access Rules

| Action | DRIVER | ADMIN |
|---|---:|---:|
| View stations | yes | yes |
| View availability | yes | yes |
| View own bookings | yes | yes |
| View all bookings | no | yes |
| Create booking | yes | yes |
| Modify own future booking | yes | yes |
| Cancel own future booking | yes | yes |
| Manage stations | no | yes |
| Manage connectors | no | yes |

## Output

- `AuthFilter.java`

---

# Phase 11: Logging Filter

## Goal

Meet the cloud deployment logging requirement.

## Create

`LoggingFilter.java`

## Log These Values

- timestamp
- HTTP method
- requested URI
- response status code
- processing time
- instance identifier

## Heroku Instance Identifier

Use:

```java
System.getenv("DYNO")
```

## Output

- `LoggingFilter.java`

---

# Phase 12: Frontend Login

## Goal

Create the first working frontend feature.

## Requirements

Create a login page that:

1. Sends username and password to `/api/auth/login`.
2. Stores the returned token.
3. Stores the returned role.
4. Sends the token in future API requests.
5. Shows different options for DRIVER and ADMIN.

## Output Files

- `index.html`
- `style.css`
- `app.js`

---

# Phase 13: Frontend Map and Station Browsing

## Goal

Allow users to browse charging stations.

## Requirements

1. Use Leaflet.js.
2. Call `GET /api/stations`.
3. Display stations as map markers.
4. When a marker is clicked, call `GET /api/stations/{id}`.
5. Show station details and connectors.

## Output

- Map UI
- Station details UI

---

# Phase 14: Frontend Availability and Booking

## Goal

Allow drivers to create bookings.

## Requirements

1. User selects connector.
2. User selects date.
3. Frontend calls availability endpoint.
4. Available slots are displayed.
5. User selects a slot.
6. Frontend calls `POST /api/bookings`.
7. If booking succeeds, show success message.
8. If conflict occurs, show suitable error.

## Output

- Availability UI
- Booking creation UI

---

# Phase 15: Frontend Booking Management

## Goal

Allow users to manage bookings.

## Requirements

1. Call `GET /api/bookings`.
2. DRIVER sees own bookings.
3. ADMIN sees all bookings.
4. Allow modifying future bookings.
5. Allow cancelling future bookings.
6. Hide or disable actions for past/already-started bookings.

## Output

- Bookings list
- Modify booking form
- Cancel booking button

---

# Phase 16: Frontend Admin Screens

## Goal

Allow admins to manage stations and connectors.

## Requirements

Only show these controls if role is `ADMIN`.

Admin can:

1. Create station.
2. Update station.
3. Delete station.
4. Add connector to station.
5. Update connector.
6. Delete connector.

## Output

- Admin station management UI
- Admin connector management UI

---

# Phase 17: Heroku Deployment

## Goal

Deploy the app to a PaaS platform.

## Requirements

Create:

- `pom.xml`
- `Procfile`

Use:

- Heroku
- Heroku Postgres
- environment/config variables

## Important

The deployed app must not use hardcoded database credentials.

The app must read database config from environment variables.

## Output

- working Heroku deployment
- deployed URL
- database add-on connected

---

# Phase 18: Testing

## Goal

Check the main marking criteria.

## Required Tests

Test the following:

1. Valid login works.
2. Invalid login fails.
3. Driver cannot see another driver's bookings.
4. Admin can see all bookings.
5. Driver can create a booking.
6. Double booking same connector/time returns `409 Conflict`.
7. Same driver overlapping booking returns `409 Conflict`.
8. Booking with `end_time <= start_time` returns `400 Bad Request`.
9. Cancelling an already-started booking is blocked.
10. Driver cannot manage stations.
11. Admin can create/update/delete stations.
12. Admin can create/update/delete connectors.
13. App reads DB config from environment variables.
14. Logging filter records required request information.

## Output

- test notes
- screenshots
- optional Postman collection

---

# Phase 19: Report Preparation

## Goal

Write the 2000-3000 word report.

## Suggested Sections

1. Introduction
2. Requirements summary
3. Server-side design
4. Database design
5. Client-side design
6. Authentication and RBAC
7. Booking rules and concurrency protection
8. Cloud deployment
9. Externalised configuration
10. Multi-instance/stateless design
11. Testing
12. Challenges and decisions
13. Conclusion

## Important Things to Explain

Explain:

- why the system uses a 3-tier architecture
- why JDBC/PostgreSQL was used
- why slots are generated dynamically
- how double bookings are prevented
- how tokens support statelessness
- how Heroku environment variables support cloud deployment
- how logging supports cloud monitoring

---

# Phase 20: Demo Video

## Goal

Prepare a short video showing the working system.

## Demo Order

1. Show login as DRIVER.
2. Show map with stations.
3. Select station and connector.
4. View available slots.
5. Create booking.
6. Try double booking and show conflict.
7. View own bookings.
8. Modify or cancel a future booking.
9. Log in as ADMIN.
10. Show all bookings.
11. Create/update station or connector.
12. Briefly show Heroku deployed app and logs.

---

# Recommended AI Build Order

Use AI in this order:

1. Generate database schema.
2. Generate database connection class.
3. Generate model classes.
4. Generate DAO classes.
5. Generate booking transaction logic.
6. Generate availability logic.
7. Generate REST resources.
8. Generate auth filter.
9. Generate logging filter.
10. Generate frontend login.
11. Generate frontend map.
12. Generate booking UI.
13. Generate admin UI.
14. Generate Heroku files.
15. Generate test plan.
16. Generate report outline.

Do not ask the AI to build the entire project in one prompt.

Build one phase at a time and test each phase before moving to the next.