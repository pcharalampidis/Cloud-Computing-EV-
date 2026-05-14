package com.evcharging.dao;

import com.evcharging.db.DatabaseManager;
import com.evcharging.exception.BookingAccessException;
import com.evcharging.exception.BookingConflictException;
import com.evcharging.exception.BookingNotFoundException;
import com.evcharging.exception.InvalidBookingException;
import com.evcharging.model.Booking;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    public List<Booking> findVisibleBookings(String username, String role) throws Exception {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return findAll();
        }
        return findByUsername(username);
    }

    public List<Booking> findAll() throws Exception {
        String sql = """
                SELECT booking_id, username, station_id, connector_id,
                       booking_date, start_time, end_time, status
                FROM bookings
                ORDER BY booking_date DESC, start_time DESC
                """;

        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                bookings.add(mapBooking(rs));
            }
        }

        return bookings;
    }

    public List<Booking> findByUsername(String username) throws Exception {
        String sql = """
                SELECT booking_id, username, station_id, connector_id,
                       booking_date, start_time, end_time, status
                FROM bookings
                WHERE username = ?
                ORDER BY booking_date DESC, start_time DESC
                """;

        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        }

        return bookings;
    }

    public Booking findById(int bookingId) throws Exception {
        String sql = """
                SELECT booking_id, username, station_id, connector_id,
                       booking_date, start_time, end_time, status
                FROM bookings
                WHERE booking_id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookingId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapBooking(rs);
                }
            }
        }

        return null;
    }

    public Booking createBooking(String username, Booking booking) throws Exception {
        validateTimes(booking.getStartTime(), booking.getEndTime());

        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                conn.setAutoCommit(false);

                lockUser(conn, username);
                int stationId = lockConnectorAndReturnStationId(conn, booking.getConnectorId());

                if (hasConnectorOverlap(conn, null, booking.getConnectorId(),
                        booking.getBookingDate(), booking.getStartTime(), booking.getEndTime())) {
                    throw new BookingConflictException("Connector already booked for this time.");
                }

                if (hasDriverOverlap(conn, null, username,
                        booking.getBookingDate(), booking.getStartTime(), booking.getEndTime())) {
                    throw new BookingConflictException("Driver already has an overlapping booking.");
                }

                String sql = """
                        INSERT INTO bookings(username, station_id, connector_id,
                                             booking_date, start_time, end_time, status)
                        VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, username);
                    stmt.setInt(2, stationId);
                    stmt.setInt(3, booking.getConnectorId());
                    stmt.setDate(4, Date.valueOf(booking.getBookingDate()));
                    stmt.setTime(5, Time.valueOf(booking.getStartTime()));
                    stmt.setTime(6, Time.valueOf(booking.getEndTime()));

                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            booking.setBookingId(keys.getInt(1));
                        }
                    }
                }

                booking.setUsername(username);
                booking.setStationId(stationId);
                booking.setStatus("ACTIVE");

                conn.commit();
                return booking;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Booking updateBooking(int bookingId, Booking updatedBooking,
                                 String requesterUsername, String requesterRole) throws Exception {

        validateTimes(updatedBooking.getStartTime(), updatedBooking.getEndTime());

        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                conn.setAutoCommit(false);

                Booking existing = lockBooking(conn, bookingId);

                if (existing == null) {
                    throw new BookingNotFoundException("Booking not found.");
                }

                if (!"ADMIN".equalsIgnoreCase(requesterRole)
                        && !existing.getUsername().equals(requesterUsername)) {
                    throw new BookingAccessException("You can only modify your own bookings.");
                }

                if (!"ACTIVE".equals(existing.getStatus())) {
                    throw new InvalidBookingException("Only ACTIVE bookings can be modified.");
                }

                if (hasStarted(existing)) {
                    throw new InvalidBookingException("Booking has already started and cannot be modified.");
                }

                lockUser(conn, existing.getUsername());
                int newStationId = lockConnectorAndReturnStationId(conn, updatedBooking.getConnectorId());

                if (hasConnectorOverlap(conn, bookingId, updatedBooking.getConnectorId(),
                        updatedBooking.getBookingDate(), updatedBooking.getStartTime(), updatedBooking.getEndTime())) {
                    throw new BookingConflictException("Connector already booked for this time.");
                }

                if (hasDriverOverlap(conn, bookingId, existing.getUsername(),
                        updatedBooking.getBookingDate(), updatedBooking.getStartTime(), updatedBooking.getEndTime())) {
                    throw new BookingConflictException("Driver already has an overlapping booking.");
                }

                String sql = """
                        UPDATE bookings
                        SET station_id = ?, connector_id = ?, booking_date = ?,
                            start_time = ?, end_time = ?
                        WHERE booking_id = ?
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, newStationId);
                    stmt.setInt(2, updatedBooking.getConnectorId());
                    stmt.setDate(3, Date.valueOf(updatedBooking.getBookingDate()));
                    stmt.setTime(4, Time.valueOf(updatedBooking.getStartTime()));
                    stmt.setTime(5, Time.valueOf(updatedBooking.getEndTime()));
                    stmt.setInt(6, bookingId);
                    stmt.executeUpdate();
                }

                updatedBooking.setBookingId(bookingId);
                updatedBooking.setUsername(existing.getUsername());
                updatedBooking.setStationId(newStationId);
                updatedBooking.setStatus("ACTIVE");

                conn.commit();
                return updatedBooking;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean cancelBooking(int bookingId, String requesterUsername, String requesterRole) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                conn.setAutoCommit(false);

                Booking existing = lockBooking(conn, bookingId);

                if (existing == null) {
                    throw new BookingNotFoundException("Booking not found.");
                }

                if (!"ADMIN".equalsIgnoreCase(requesterRole)
                        && !existing.getUsername().equals(requesterUsername)) {
                    throw new BookingAccessException("You can only cancel your own bookings.");
                }

                if (!"ACTIVE".equals(existing.getStatus())) {
                    throw new InvalidBookingException("Only ACTIVE bookings can be cancelled.");
                }

                if (hasStarted(existing)) {
                    throw new InvalidBookingException("Booking has already started and cannot be cancelled.");
                }

                String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, bookingId);
                    stmt.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void lockUser(Connection conn, String username) throws Exception {
        String sql = "SELECT username FROM users WHERE username = ? FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new InvalidBookingException("User does not exist.");
                }
            }
        }
    }

    private int lockConnectorAndReturnStationId(Connection conn, int connectorId) throws Exception {
        String sql = "SELECT connector_id, station_id FROM connectors WHERE connector_id = ? FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, connectorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("station_id");
                }
            }
        }

        throw new InvalidBookingException("Connector does not exist.");
    }

    private Booking lockBooking(Connection conn, int bookingId) throws Exception {
        String sql = """
                SELECT booking_id, username, station_id, connector_id,
                       booking_date, start_time, end_time, status
                FROM bookings
                WHERE booking_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookingId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapBooking(rs);
                }
            }
        }

        return null;
    }

    private boolean hasConnectorOverlap(Connection conn, Integer ignoredBookingId, int connectorId,
                                        java.time.LocalDate date, LocalTime start, LocalTime end) throws Exception {
        String sql = """
                SELECT 1
                FROM bookings
                WHERE connector_id = ?
                AND booking_date = ?
                AND status = 'ACTIVE'
                AND start_time < ?
                AND end_time > ?
                """;

        if (ignoredBookingId != null) {
            sql += " AND booking_id <> ?";
        }

        sql += " LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, connectorId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setTime(3, Time.valueOf(end));
            stmt.setTime(4, Time.valueOf(start));

            if (ignoredBookingId != null) {
                stmt.setInt(5, ignoredBookingId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasDriverOverlap(Connection conn, Integer ignoredBookingId, String username,
                                     java.time.LocalDate date, LocalTime start, LocalTime end) throws Exception {
        String sql = """
                SELECT 1
                FROM bookings
                WHERE username = ?
                AND booking_date = ?
                AND status = 'ACTIVE'
                AND start_time < ?
                AND end_time > ?
                """;

        if (ignoredBookingId != null) {
            sql += " AND booking_id <> ?";
        }

        sql += " LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setTime(3, Time.valueOf(end));
            stmt.setTime(4, Time.valueOf(start));

            if (ignoredBookingId != null) {
                stmt.setInt(5, ignoredBookingId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void validateTimes(LocalTime start, LocalTime end) throws InvalidBookingException {
        if (start == null || end == null) {
            throw new InvalidBookingException("Start time and end time are required.");
        }

        if (!end.isAfter(start)) {
            throw new InvalidBookingException("End time must be after start time.");
        }
    }

    private boolean hasStarted(Booking booking) {
        LocalDateTime bookingStart = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getStartTime()
        );

        return !LocalDateTime.now().isBefore(bookingStart);
    }

    private Booking mapBooking(ResultSet rs) throws Exception {
        return new Booking(
                rs.getInt("booking_id"),
                rs.getString("username"),
                rs.getInt("station_id"),
                rs.getInt("connector_id"),
                rs.getDate("booking_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getString("status")
        );
    }
}