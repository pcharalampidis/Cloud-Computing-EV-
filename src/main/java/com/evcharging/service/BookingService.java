package com.evcharging.service;

import com.evcharging.dao.BookingDAO;
import com.evcharging.dto.BookingRequest;
import com.evcharging.dto.BookingResponse;
import com.evcharging.exception.InvalidBookingException;
import com.evcharging.model.Booking;
import com.evcharging.security.CurrentUser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    private final BookingDAO bookingDAO = new BookingDAO();

    public List<BookingResponse> getVisibleBookings(CurrentUser user) throws Exception {
        List<Booking> bookings = bookingDAO.findVisibleBookings(
                user.getUsername(),
                user.getRole()
        );

        List<BookingResponse> response = new ArrayList<>();

        for (Booking booking : bookings) {
            response.add(new BookingResponse(booking));
        }

        return response;
    }

    public BookingResponse createBooking(CurrentUser user, BookingRequest request) throws Exception {
        Booking booking = toBooking(request);

        String bookingUsername = user.getUsername();

        if (user.isAdmin() &&
                request.getUsername() != null &&
                !request.getUsername().isBlank()) {
            bookingUsername = request.getUsername();
        }

        Booking created = bookingDAO.createBooking(bookingUsername, booking);

        return new BookingResponse(created);
    }

    public BookingResponse updateBooking(int bookingId, CurrentUser user, BookingRequest request) throws Exception {
        Booking updatedBooking = toBooking(request);

        Booking updated = bookingDAO.updateBooking(
                bookingId,
                updatedBooking,
                user.getUsername(),
                user.getRole()
        );

        return new BookingResponse(updated);
    }

    public void cancelBooking(int bookingId, CurrentUser user) throws Exception {
        bookingDAO.cancelBooking(
                bookingId,
                user.getUsername(),
                user.getRole()
        );
    }

    private Booking toBooking(BookingRequest request) throws InvalidBookingException {
        if (request == null) {
            throw new InvalidBookingException("Booking request is required.");
        }

        if (request.getConnectorId() <= 0) {
            throw new InvalidBookingException("Valid connector_id is required.");
        }

        if (request.getBookingDate() == null ||
                request.getStartTime() == null ||
                request.getEndTime() == null) {
            throw new InvalidBookingException("booking_date, start_time and end_time are required.");
        }

        Booking booking = new Booking();
        booking.setConnectorId(request.getConnectorId());
        booking.setBookingDate(LocalDate.parse(request.getBookingDate()));
        booking.setStartTime(LocalTime.parse(request.getStartTime()));
        booking.setEndTime(LocalTime.parse(request.getEndTime()));

        return booking;
    }
}