package com.evcharging.dao;

import com.evcharging.db.DatabaseManager;
import com.evcharging.model.Booking;
import com.evcharging.model.TimeSlot;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityDAO {

    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(21, 0);

    public List<TimeSlot> getAvailableSlots(int connectorId, LocalDate date) throws Exception {
        List<TimeSlot> allSlots = generateHourlySlots();
        List<Booking> activeBookings = findActiveBookingsForConnector(connectorId, date);

        List<TimeSlot> availableSlots = new ArrayList<>();

        for (TimeSlot slot : allSlots) {
            boolean overlaps = false;

            for (Booking booking : activeBookings) {
                if (overlaps(slot.getStartTime(), slot.getEndTime(),
                        booking.getStartTime(), booking.getEndTime())) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                availableSlots.add(slot);
            }
        }

        return availableSlots;
    }

    private List<TimeSlot> generateHourlySlots() {
        List<TimeSlot> slots = new ArrayList<>();

        LocalTime current = OPENING_TIME;

        while (current.plusHours(1).compareTo(CLOSING_TIME) <= 0) {
            LocalTime end = current.plusHours(1);
            slots.add(new TimeSlot(current, end));
            current = end;
        }

        return slots;
    }

    private List<Booking> findActiveBookingsForConnector(int connectorId, LocalDate date) throws Exception {
        String sql = """
                SELECT booking_id, username, station_id, connector_id,
                       booking_date, start_time, end_time, status
                FROM bookings
                WHERE connector_id = ?
                AND booking_date = ?
                AND status = 'ACTIVE'
                ORDER BY start_time
                """;

        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, connectorId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        }

        return bookings;
    }

    private boolean overlaps(LocalTime start1, LocalTime end1,
                             LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
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