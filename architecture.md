# System Architecture

The system follows a simple 3-tier architecture, matching the module material on REST, Servlets/JAX-RS, SOA, JDBC, and PaaS deployment.

---

## 1. Presentation Tier

### Technology
* HTML
* CSS
* Vanilla JavaScript
* Leaflet.js for the map

### Responsibilities
The frontend allows users to:

* log in
* view charging stations on a map
* inspect station and connector details
* view available slots
* create bookings
* view bookings
* modify or cancel bookings
* access admin station/connector management screens if the user is an ADMIN

### Communication
The frontend communicates with the backend using asynchronous HTTP requests with `fetch()`.

Example:

```javascript
fetch("/api/stations", {
  headers: {
    "Authorization": "Bearer " + token
  }
});
```

The UI adapts depending on the logged-in user's role:
* DRIVER: booking features only
* ADMIN: booking features plus station/connector management

---

## 2. Application Tier

### Technology
* Java
* Jersey JAX-RS
* Java Servlet container such as Tomcat
* Raw JDBC for database access

### REST Controllers
JAX-RS resource classes expose the REST API.

Example classes:

* `AuthResource`
* `StationResource`
* `ConnectorResource`
* `AvailabilityResource`
* `BookingResource`

These use annotations such as:

```java
@Path
@GET
@POST
@PUT
@DELETE
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
```

### DAO Layer
DAO classes contain JDBC database logic.

Example DAO classes:

* `UserDAO`
* `TokenDAO`
* `StationDAO`
* `ConnectorDAO`
* `BookingDAO`

The DAO layer keeps SQL code separate from the REST resource classes.

### Authentication Filter
An `AuthFilter` implements `ContainerRequestFilter`.

Responsibilities:
* read the `Authorization` header
* validate the token against the database
* identify the username and role
* reject invalid requests with `401 Unauthorized`
* reject forbidden role access with `403 Forbidden`

No Java server sessions are used.

### RBAC
Role-based access control is enforced in the filter and/or resource methods.

Example:
* only ADMIN can call `POST /api/stations`
* DRIVER can only access their own bookings
* ADMIN can view and manage all future bookings

### Logging Filter
A `LoggingFilter` implements:

* `ContainerRequestFilter`
* `ContainerResponseFilter`

It logs:
* timestamp
* HTTP method
* requested URI
* response status code
* processing time
* Heroku dyno/instance identifier using `System.getenv("DYNO")`

---

## 3. Data Tier

### Technology
* PostgreSQL
* JDBC

### Responsibilities
The database stores:
* users
* authentication tokens
* charging stations
* connectors
* bookings

### Externalised Configuration
Database configuration is loaded from environment variables.

Example:
* `JDBC_DATABASE_URL`
* `JDBC_DATABASE_USERNAME`
* `JDBC_DATABASE_PASSWORD`

This is suitable for Heroku deployment because credentials are provided through config vars.

---

## 4. Stateless Cloud-Ready Design

The application is stateless because:
* it does not use Java server sessions
* each request contains the auth token
* the token is validated using the shared database
* any Heroku dyno can serve any request

This supports multi-instance execution on a PaaS platform.

---

## 5. Concurrency Design

The main concurrency risk is two users booking the same connector at the same time.

The simple transaction strategy is:

1. Open database connection.
2. Disable auto-commit.
3. Lock the relevant user row:

```sql
SELECT username FROM users WHERE username = ? FOR UPDATE;
```

4. Lock the relevant connector row:

```sql
SELECT connector_id, station_id FROM connectors WHERE connector_id = ? FOR UPDATE;
```

5. Check for overlapping ACTIVE bookings for the connector.
6. Check for overlapping ACTIVE bookings for the driver.
7. Insert or update the booking.
8. Commit.
9. Roll back if a rule fails.

This avoids using `SELECT COUNT(*) ... FOR UPDATE`, which is not a good PostgreSQL locking pattern.

---

## 6. Slot Model

The project does not store available slots in a table.

Instead:
* the application has fixed opening hours
* the backend generates hourly slots for the requested date
* existing ACTIVE bookings are removed from the result

This is simple and realistic for a student coursework project.