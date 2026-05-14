# Use Cases

## Actor: DRIVER

### UC1: Authenticate
The driver enters a username and password.

Main flow:
1. Driver submits credentials.
2. Server checks the password hash.
3. Server creates an auth token.
4. Server returns the token and user role.

Result:
* Driver is logged in.

---

### UC2: View Map
The driver opens the application and views charging stations on a map.

Main flow:
1. Frontend calls `GET /api/stations`.
2. Server returns station data.
3. Frontend displays markers using Leaflet.js.

Result:
* Driver can browse station locations.

---

### UC3: Inspect Station
The driver selects a station marker.

Main flow:
1. Frontend calls `GET /api/stations/{id}`.
2. Server returns station details and connectors.
3. Frontend displays connector types.

Result:
* Driver can inspect station details.

---

### UC4: Check Availability
The driver selects a connector and date.

Main flow:
1. Frontend calls `GET /api/connectors/{id}/availability?date=YYYY-MM-DD`.
2. Server generates hourly slots from fixed opening hours.
3. Server removes slots that overlap ACTIVE bookings.
4. Server returns free slots.

Result:
* Driver can see available charging slots.

---

### UC5: Create Booking
The driver selects a free slot and confirms the booking.

Main flow:
1. Frontend calls `POST /api/bookings`.
2. Server starts a database transaction.
3. Server locks the user row and connector row.
4. Server checks:
   * `end_time > start_time`
   * no overlapping ACTIVE booking for the same connector
   * no overlapping ACTIVE booking for the same driver
5. Server inserts the booking.
6. Server commits the transaction.

Alternative flow:
* If overlap exists, server returns `409 Conflict`.

Result:
* Booking is created safely.

---

### UC6: View Own Bookings
The driver views their bookings.

Main flow:
1. Frontend calls `GET /api/bookings`.
2. Server checks the user's role.
3. If DRIVER, server returns only bookings belonging to that username.

Result:
* Driver cannot see other drivers' bookings.

---

### UC7: Modify Own Future Booking
The driver modifies one of their bookings.

Main flow:
1. Driver selects an existing booking.
2. Frontend calls `PUT /api/bookings/{id}`.
3. Server checks ownership.
4. Server checks that the booking has not started.
5. Server checks for connector and driver overlap.
6. Server updates the booking.

Alternative flows:
* If the booking belongs to another driver, return `403 Forbidden`.
* If the booking has already started, return `409 Conflict`.
* If the new time overlaps, return `409 Conflict`.

Result:
* Driver can modify only their own future bookings.

---

### UC8: Cancel Own Future Booking
The driver cancels a booking.

Main flow:
1. Driver selects a booking.
2. Frontend calls `DELETE /api/bookings/{id}`.
3. Server checks ownership.
4. Server checks that the booking has not started.
5. Server changes status to `CANCELLED`.

Alternative flows:
* If the booking belongs to another driver, return `403 Forbidden`.
* If the booking has already started, return `409 Conflict`.

Result:
* Driver can cancel only their own future bookings.

---

## Actor: ADMIN

### UC9: Admin Login
The admin logs in using username and password.

Result:
* Admin receives a token with role `ADMIN`.

---

### UC10: Manage Charging Stations
The admin creates, updates, or deletes stations.

Main operations:
* `POST /api/stations`
* `PUT /api/stations/{id}`
* `DELETE /api/stations/{id}`

Result:
* Admin can manage charging station data.

---

### UC11: Manage Connectors
The admin creates, updates, or deletes connectors.

Main operations:
* `POST /api/stations/{id}/connectors`
* `PUT /api/connectors/{id}`
* `DELETE /api/connectors/{id}`

Result:
* Admin can manage connector data.

---

### UC12: Manage Bookings
The admin views and manages bookings.

Main flow:
1. Admin calls `GET /api/bookings`.
2. Server returns all bookings.
3. Admin can modify or cancel any future booking.

Result:
* Admin can manage bookings without being restricted by ownership.
* The booking start-time rule still applies for simplicity and consistency.