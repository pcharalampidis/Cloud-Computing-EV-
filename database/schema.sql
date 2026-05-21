-- PostgreSQL schema for EV Charging Booking System

DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS auth_tokens;
DROP TABLE IF EXISTS connectors;
DROP TABLE IF EXISTS stations;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT chk_user_role CHECK (role IN ('DRIVER', 'ADMIN'))
);

CREATE TABLE auth_tokens (
    token VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_token_user
        FOREIGN KEY (username)
        REFERENCES users(username)
        ON DELETE CASCADE
);

CREATE TABLE stations (
    station_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL
);

CREATE TABLE connectors (
    connector_id SERIAL PRIMARY KEY,
    station_id INT NOT NULL,
    connector_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_connector_station
        FOREIGN KEY (station_id)
        REFERENCES stations(station_id)
        ON DELETE CASCADE
);

CREATE TABLE bookings (
    booking_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    station_id INT NOT NULL,
    connector_id INT NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_booking_user
        FOREIGN KEY (username)
        REFERENCES users(username),

    CONSTRAINT fk_booking_station
        FOREIGN KEY (station_id)
        REFERENCES stations(station_id),

    CONSTRAINT fk_booking_connector
        FOREIGN KEY (connector_id)
        REFERENCES connectors(connector_id),

    CONSTRAINT chk_booking_status
        CHECK (status IN ('ACTIVE', 'CANCELLED')),

    CONSTRAINT chk_booking_time
        CHECK (end_time > start_time)
);

-- Helpful indexes for booking overlap checks
CREATE INDEX idx_bookings_connector_date_status
ON bookings(connector_id, booking_date, status);

CREATE INDEX idx_bookings_username_date_status
ON bookings(username, booking_date, status);

-- Mock users
-- For simplicity, all mock passwords are: password
-- SHA-256 hash of "password":
-- 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8

INSERT INTO users(username, password_hash, role) VALUES
('admin', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'ADMIN'),
('driver1', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'DRIVER'),
('driver2', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'DRIVER'),
('driver3', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'DRIVER'),
('driver4', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'DRIVER'),
('driver5', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'DRIVER');

-- Mock stations
INSERT INTO stations(name, address, latitude, longitude) VALUES
('City Centre Charger', '10 Tsimiski Street, Thessaloniki', 40.63290000, 22.94080000),
('University Charger', 'University Campus, Thessaloniki', 40.62930000, 22.95840000),
('Seafront Charger', 'Nikis Avenue, Thessaloniki', 40.62630000, 22.94840000),
('Airport Supercharger', 'Makedonia Airport, Thessaloniki', 40.52440000, 22.97500000),
('Cosmos Mall Charger', '11th km Thessaloniki - Moudania, Thessaloniki', 40.55570000, 22.99610000);

-- Mock connectors
INSERT INTO connectors(station_id, connector_type) VALUES
(1, 'Type 2'),
(1, 'CCS'),
(2, 'Type 2'),
(2, 'CHAdeMO'),
(3, 'CCS'),
(4, 'CCS'),
(4, 'CCS'),
(4, 'Type 2'),
(5, 'Type 2'),
(5, 'Type 2');

-- Mock bookings
INSERT INTO bookings(username, station_id, connector_id, booking_date, start_time, end_time, status) VALUES
('driver1', 1, 1, '2026-05-20', '10:00', '11:00', 'ACTIVE'),
('driver2', 1, 2, '2026-05-20', '12:00', '13:00', 'ACTIVE'),
('driver3', 4, 6, '2026-05-21', '09:00', '10:30', 'ACTIVE'),
('driver4', 4, 7, '2026-05-21', '09:30', '11:00', 'ACTIVE'),
('driver5', 5, 9, '2026-05-22', '14:00', '16:00', 'ACTIVE'),
('driver1', 5, 10, '2026-05-22', '14:30', '15:30', 'ACTIVE'),
('driver2', 2, 3, '2026-05-23', '08:00', '09:00', 'ACTIVE');