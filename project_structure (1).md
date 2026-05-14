# Recommended Project File Structure

Use a standard Maven Java web application structure.

```text
ev-charging-booking/
│
├── pom.xml
├── Procfile
├── README.md
│
├── database/
│   └── schema.sql
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── evcharging/
│   │   │           │
│   │   │           ├── config/
│   │   │           │   └── EVChargingApplication.java
│   │   │           │
│   │   │           ├── db/
│   │   │           │   └── DatabaseManager.java
│   │   │           │
│   │   │           ├── model/
│   │   │           │   ├── User.java
│   │   │           │   ├── AuthToken.java
│   │   │           │   ├── Station.java
│   │   │           │   ├── Connector.java
│   │   │           │   ├── Booking.java
│   │   │           │   └── TimeSlot.java
│   │   │           │
│   │   │           ├── dto/
│   │   │           │   ├── LoginRequest.java
│   │   │           │   ├── LoginResponse.java
│   │   │           │   ├── ErrorResponse.java
│   │   │           │   ├── BookingRequest.java
│   │   │           │   ├── ConnectorRequest.java
│   │   │           │   ├── StationDetailsResponse.java
│   │   │           │   └── TimeSlotResponse.java
│   │   │           │
│   │   │           ├── dao/
│   │   │           │   ├── UserDAO.java
│   │   │           │   ├── TokenDAO.java
│   │   │           │   ├── StationDAO.java
│   │   │           │   ├── ConnectorDAO.java
│   │   │           │   ├── BookingDAO.java
│   │   │           │   └── AvailabilityDAO.java
│   │   │           │
│   │   │           ├── resource/
│   │   │           │   ├── AuthResource.java
│   │   │           │   ├── HealthResource.java
│   │   │           │   ├── StationResource.java
│   │   │           │   ├── ConnectorResource.java
│   │   │           │   ├── AvailabilityResource.java
│   │   │           │   └── BookingResource.java
│   │   │           │
│   │   │           ├── filter/
│   │   │           │   ├── AuthFilter.java
│   │   │           │   └── LoggingFilter.java
│   │   │           │
│   │   │           ├── security/
│   │   │           │   └── CurrentUser.java
│   │   │           │
│   │   │           ├── exception/
│   │   │           │   ├── BookingConflictException.java
│   │   │           │   ├── InvalidBookingException.java
│   │   │           │   ├── BookingNotFoundException.java
│   │   │           │   └── BookingAccessException.java
│   │   │           │
│   │   │           └── util/
│   │   │               └── PasswordUtil.java
│   │   │
│   │   ├── resources/
│   │   │   └── logback.xml              optional
│   │   │
│   │   └── webapp/
│   │       ├── index.html
│   │       ├── css/
│   │       │   └── style.css
│   │       ├── js/
│   │       │   └── app.js
│   │       └── WEB-INF/
│   │           └── web.xml              optional
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── evcharging/
│                   └── AppTest.java
```

## Important Notes

### 1. Package names must match folders

Example:

```java
package com.evcharging.dao;
```

must be inside:

```text
src/main/java/com/evcharging/dao/
```

### 2. SQL file location

Keep:

```text
database/schema.sql
```

outside `src` because you will manually run it in PostgreSQL or Heroku Postgres.

### 3. Frontend location

Put the frontend files in:

```text
src/main/webapp/
```

So later:

```text
index.html
css/style.css
js/app.js
```

will be served by the Java web app.

### 4. Backend implementation order

Continue creating backend files first:

1. DAOs
2. Resources/controllers
3. Filters
4. `pom.xml`
5. Test backend with Postman/browser
6. Then frontend

### 5. Optional `web.xml`

Because we already have:

```java
@ApplicationPath("/api")
```

in `EVChargingApplication.java`, `web.xml` may not be needed.

If Jersey does not auto-detect the app later, we can add a simple `web.xml`.