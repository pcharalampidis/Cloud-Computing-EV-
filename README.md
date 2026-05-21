# EV Charging Booking System

This is a RESTful API and Web UI for booking Electric Vehicle (EV) charging slots.

## 🚀 Local Development Setup

You can run this application locally without deploying it to Heroku. You will need:
1. Java 21
2. Maven
3. PostgreSQL

### 1. Set Up Local PostgreSQL Database

1. Open your PostgreSQL command line tool (`psql`) or a GUI tool like pgAdmin or DBeaver.
2. Create a new database for the project (e.g., `ev_booking`).
3. Run the schema creation script provided in `database/schema.sql` against your new database. This will create the tables and insert some mock data (users, stations, connectors, and active bookings).

### 2. Configure Environment Variables

The application relies on environment variables for its database connection. Set the following variables in your environment before running the app.

**Option A: Standard JDBC Variables (Recommended)**
- `JDBC_DATABASE_URL`: Your JDBC connection string (e.g., `jdbc:postgresql://localhost:5432/ev_booking`)
- `JDBC_DATABASE_USERNAME`: Your PostgreSQL username (e.g., `postgres`)
- `JDBC_DATABASE_PASSWORD`: Your PostgreSQL password

**Option B: Heroku-style URL**
- `DATABASE_URL`: E.g., `postgres://username:password@localhost:5432/ev_booking`

**How to set them in Windows (PowerShell):**
```powershell
$env:JDBC_DATABASE_URL="jdbc:postgresql://localhost:5432/ev_booking"
$env:JDBC_DATABASE_USERNAME="postgres"
$env:JDBC_DATABASE_PASSWORD="your_password_here"
```

### 3. Run the Application

We have configured the `jetty-maven-plugin` to easily run the application locally.

1. Open your terminal in the root of the project directory.
2. Build the project:
   ```bash
   mvn clean compile
   ```
3. Start the server:
   ```bash
   mvn jetty:run
   ```

### 4. Access the Application

Once Jetty starts, you can access the application in your browser:
- **Frontend UI:** [http://localhost:8080](http://localhost:8080)
- **API Endpoint Example:** [http://localhost:8080/api/stations](http://localhost:8080/api/stations) (Note: Most API endpoints require authentication via the UI)

### Login Credentials

The `schema.sql` file creates some default mock users. The password for all mock users is `password`.

- **Admin User:** `admin` / `password`
- **Driver Users:** `driver1` / `password`, `driver2` / `password`

---

## ☁️ Deployment (Heroku)

This app is designed to be deployed to Heroku.

1. Ensure the PostgreSQL add-on is attached to your Heroku app. Heroku will automatically inject the `DATABASE_URL` environment variable.
2. The application uses the `Procfile` to start via `webapp-runner.jar`.
3. You will need to manually run the `database/schema.sql` script on your Heroku Postgres database (e.g., using `heroku pg:psql`).
