# Database Schema

The database is designed for PostgreSQL and raw JDBC.

## Table: users

| Column | Type | Constraints |
|---|---|---|
| username | VARCHAR(50) | Primary Key |
| password_hash | VARCHAR(255) | Not Null |
| role | VARCHAR(20) | Not Null, CHECK role IN ('DRIVER', 'ADMIN') |

Purpose:
* Stores application users.
* Users can be drivers or admins.
* Passwords are stored as hashes, not plaintext.

---

## Table: auth_tokens

| Column | Type | Constraints |
|---|---|---|
| token | VARCHAR(255) | Primary Key |
| username | VARCHAR(50) | Foreign Key -> users(username), Not Null |
| created_at | TIMESTAMP | Not Null |
| expires_at | TIMESTAMP | Not Null |

Purpose:
* Stores login tokens.
* Keeps authentication stateless because any server instance can check the token in the database.
* Avoids Java server sessions.

---

## Table: stations

| Column | Type | Constraints |
|---|---|---|
| station_id | SERIAL | Primary Key |
| name | VARCHAR(100) | Not Null |
| address | VARCHAR(255) | Not Null |
| latitude | DECIMAL(10,8) | Not Null |
| longitude | DECIMAL(11,8) | Not Null |

Purpose:
* Stores charging station details.
* Coordinates are used by the frontend map.

---

## Table: connectors

| Column | Type | Constraints |
|---|---|---|
| connector_id | SERIAL | Primary Key |
| station_id | INT | Foreign Key -> stations(station_id), Not Null |
| connector_type | VARCHAR(50) | Not Null |

Purpose:
* Stores the connectors available at each charging station.
* Example connector types: Type 2, CCS, CHAdeMO.

---

## Table: bookings

| Column | Type | Constraints |
|---|---|---|
| booking_id | SERIAL | Primary Key |
| username | VARCHAR(50) | Foreign Key -> users(username), Not Null |
| station_id | INT | Foreign Key -> stations(station_id), Not Null |
| connector_id | INT | Foreign Key -> connectors(connector_id), Not Null |
| booking_date | DATE | Not Null |
| start_time | TIME | Not Null |
| end_time | TIME | Not Null |
| status | VARCHAR(20) | Not Null, CHECK status IN ('ACTIVE', 'CANCELLED') |

Additional constraint:

```sql
CHECK (end_time > start_time)
```

Purpose:
* Stores reservations made by drivers.
* `status` allows cancellation without physically deleting booking history.

---

## Available Charging Slots

There is no separate `available_slots` table.

For coursework simplicity, available slots are generated dynamically by the server.

Example opening hours:
* 08:00 to 21:00
* 1-hour slots

The backend creates possible hourly slots and removes any slot that overlaps an ACTIVE booking for the selected connector and date.

Example:
* Fixed slots:
  * 08:00-09:00
  * 09:00-10:00
  * 10:00-11:00
* Existing booking:
  * 09:00-10:00
* Returned available slots:
  * 08:00-09:00
  * 10:00-11:00

---

## Booking Overlap Rule

Two time periods overlap if:

```sql
existing.start_time < requested_end_time
AND existing.end_time > requested_start_time
```

This rule is used to check:

1. Same connector overlap:

```sql
connector_id = ?
AND booking_date = ?
AND status = 'ACTIVE'
AND start_time < ?
AND end_time > ?
```

2. Same driver overlap:

```sql
username = ?
AND booking_date = ?
AND status = 'ACTIVE'
AND start_time < ?
AND end_time > ?
```

---

## Concurrency Plan

The system must prevent two users from booking the same connector at the same time.

The simple JDBC transaction plan is:

1. Start transaction.
2. Lock the relevant user row:

```sql
SELECT username FROM users WHERE username = ? FOR UPDATE;
```

3. Lock the relevant connector row:

```sql
SELECT connector_id, station_id FROM connectors WHERE connector_id = ? FOR UPDATE;
```

4. Check that `end_time > start_time`.
5. Check there is no overlapping ACTIVE booking for the same connector.
6. Check the driver has no overlapping ACTIVE booking.
7. Insert or update the booking.
8. Commit.
9. If any check fails, rollback.

Important:
* Do not use `SELECT COUNT(*) ... FOR UPDATE`.
* PostgreSQL does not allow locking aggregate result rows in a useful way.
* Locking the concrete `users` and `connectors` rows is simpler and suitable for this coursework.

---

## Suggested SQL Constraints

```sql
ALTER TABLE users
ADD CONSTRAINT chk_user_role
CHECK (role IN ('DRIVER', 'ADMIN'));

ALTER TABLE bookings
ADD CONSTRAINT chk_booking_status
CHECK (status IN ('ACTIVE', 'CANCELLED'));

ALTER TABLE bookings
ADD CONSTRAINT chk_booking_time
CHECK (end_time > start_time);
```