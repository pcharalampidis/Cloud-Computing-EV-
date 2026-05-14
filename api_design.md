# REST API Design

All request and response bodies use `application/json`.

The API is implemented using Java Jersey JAX-RS.

---

## Authentication

### POST `/api/auth/login`

Logs in a user.

Request:

```json
{
  "username": "driver1",
  "password": "password123"
}
```

Response:

```json
{
  "token": "generated-token-value",
  "username": "driver1",
  "role": "DRIVER"
}
```

Notes:
* Password is checked against the stored password hash.
* The returned token is stored in the database.
* Future requests send the token in the `Authorization` header.

Example:

```http
Authorization: Bearer generated-token-value
```

---

## Stations

### GET `/api/stations`

Lists all charging stations.

Access:
* DRIVER
* ADMIN

---

### GET `/api/stations/{id}`

Returns station details and its connectors.

Access:
* DRIVER
* ADMIN

---

### POST `/api/stations`

Creates a new station.

Access:
* ADMIN only

Request:

```json
{
  "name": "City Centre Charger",
  "address": "10 Main Street",
  "latitude": 40.640062,
  "longitude": 22.944419
}
```

---

### PUT `/api/stations/{id}`

Updates a station.

Access:
* ADMIN only

---

### DELETE `/api/stations/{id}`

Deletes a station.

Access:
* ADMIN only

Notes:
* For simplicity, deletion can be blocked if the station has connectors or bookings.
* Alternatively, the admin must delete connectors first.

---

## Connectors

### POST `/api/stations/{id}/connectors`

Adds a connector to a station.

Access:
* ADMIN only

Request:

```json
{
  "connector_type": "CCS"
}
```

---

### PUT `/api/connectors/{id}`

Updates a connector.

Access:
* ADMIN only

Request:

```json
{
  "connector_type": "Type 2"
}
```

---

### DELETE `/api/connectors/{id}`

Deletes a connector.

Access:
* ADMIN only

Notes:
* For simplicity, deletion can be blocked if the connector has bookings.

---

## Availability

### GET `/api/connectors/{id}/availability?date=YYYY-MM-DD`

Returns available hourly slots for a connector on a selected date.

Access:
* DRIVER
* ADMIN

Example response:

```json
[
  {
    "start_time": "08:00",
    "end_time": "09:00"
  },
  {
    "start_time": "10:00",
    "end_time": "11:00"
  }
]
```

Notes:
* There is no `available_slots` table.
* Slots are generated dynamically from fixed opening hours.
* ACTIVE bookings are subtracted from the generated slots.

---

## Bookings

### GET `/api/bookings`

Lists bookings.

Access:
* DRIVER: sees only own bookings
* ADMIN: sees all bookings

---

### POST `/api/bookings`

Creates a booking.

Access:
* DRIVER
* ADMIN

Request:

```json
{
  "connector_id": 1,
  "booking_date": "2026-05-20",
  "start_time": "10:00",
  "end_time": "11:00"
}
```

Response:
* `201 Created` if successful
* `400 Bad Request` if end time is not after start time
* `401 Unauthorized` if no valid token is provided
* `403 Forbidden` if the user is not allowed
* `409 Conflict` if there is an overlapping booking

Rules:
* The server obtains the station ID from the connector.
* The booking must not overlap another ACTIVE booking for the same connector.
* The driver must not already have an overlapping ACTIVE booking.
* The booking insert runs inside a database transaction.

---

### PUT `/api/bookings/{id}`

Modifies a booking.

Access:
* DRIVER: own future bookings only
* ADMIN: any future booking

Request:

```json
{
  "connector_id": 2,
  "booking_date": "2026-05-20",
  "start_time": "11:00",
  "end_time": "12:00"
}
```

Response:
* `200 OK` if successful
* `400 Bad Request` for invalid times
* `403 Forbidden` if a driver tries to modify another driver's booking
* `409 Conflict` if the new time overlaps
* `409 Conflict` or `400 Bad Request` if the booking has already started

---

### DELETE `/api/bookings/{id}`

Cancels a booking by setting status to `CANCELLED`.

Access:
* DRIVER: own future bookings only
* ADMIN: any future booking

Response:
* `204 No Content` if successful
* `403 Forbidden` if a driver tries to cancel another driver's booking
* `409 Conflict` or `400 Bad Request` if the booking has already started

---

## Access-Control Table

| Action | DRIVER | ADMIN |
|---|---:|---:|
| View stations/connectors | yes | yes |
| View own bookings | yes | yes |
| View all bookings | no | yes |
| Create own booking | yes | yes |
| Modify own future booking | yes | yes |
| Cancel own future booking | yes | yes |
| Cancel any future booking | no | yes |
| Manage stations | no | yes |
| Manage connectors | no | yes |