# System Requirements

## 1. Functional Requirements

### Authentication
* Users log in using an existing username and password.
* If credentials are valid, the server returns an authentication token.
* The token is sent in the `Authorization` header for protected API requests.
* Passwords are stored as hashes, not plaintext.

### Browsing Charging Stations
* Drivers can view all charging stations on a map.
* Drivers can select a station to view its address and connectors.

### Availability
* Drivers can select a connector and date to view available charging slots.
* Available slots are generated dynamically by the server.
* For simplicity, the system uses fixed hourly opening hours, for example:
  * 08:00-09:00
  * 09:00-10:00
  * ...
  * 20:00-21:00
* A slot is available if there is no ACTIVE booking overlapping that connector and time.

### Booking
* Drivers can reserve an available connector for a specific date and time.
* The system must prevent:
  * two ACTIVE bookings overlapping on the same connector
  * one driver having overlapping ACTIVE bookings
  * bookings where `end_time <= start_time`

### Booking Management
* Drivers can view their own bookings.
* Drivers can modify or cancel their own bookings only before the booking start time.
* Admins can view all bookings.
* Admins can manage any future booking, but past or already-started bookings are not modified or cancelled. This keeps the business rule simple and consistent.

### Admin Management
Admins can perform CRUD operations on:
* charging stations
* connectors

## 2. Non-Functional Requirements

### Architecture
* The application follows a 3-tier architecture:
  * Presentation Tier: HTML, CSS, JavaScript
  * Application Tier: Java JAX-RS REST API
  * Data Tier: PostgreSQL database

### Backend
* Java with Jersey JAX-RS.
* Raw JDBC is used for database access.
* DAO classes handle SQL operations.

### Database
* PostgreSQL is used locally and on Heroku.
* Database connection details are read from environment variables using `System.getenv()`.

### Cloud Deployment
* The application is deployed on Heroku.
* Heroku Postgres is used as the database add-on.
* The application must not depend on local files or hardcoded database credentials.

### Statelessness
* The REST API must be stateless.
* Java server sessions are not used.
* Authentication tokens are stored in the database so any server instance can validate them.

### Concurrency
* Booking creation and modification must be protected using database transactions.
* The booking transaction locks the relevant user row and connector row using `SELECT ... FOR UPDATE`.
* The system then checks for overlapping ACTIVE bookings before inserting or updating.

### Logging
Each request should be logged with:
* timestamp
* HTTP method
* requested URI
* response status code
* processing time
* Heroku instance identifier from `System.getenv("DYNO")`