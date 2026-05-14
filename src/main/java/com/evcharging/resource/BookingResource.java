package com.evcharging.resource;

import com.evcharging.dao.BookingDAO;
import com.evcharging.dto.BookingRequest;
import com.evcharging.dto.BookingResponse;
import com.evcharging.dto.ErrorResponse;
import com.evcharging.exception.BookingAccessException;
import com.evcharging.exception.BookingConflictException;
import com.evcharging.exception.BookingNotFoundException;
import com.evcharging.exception.InvalidBookingException;
import com.evcharging.filter.AuthFilter;
import com.evcharging.model.Booking;
import com.evcharging.security.CurrentUser;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    private final BookingDAO bookingDAO = new BookingDAO();

    @Context
    private ContainerRequestContext requestContext;

    @GET
    public Response getBookings() {
        try {
            CurrentUser user = getCurrentUser();

            List<Booking> bookings = bookingDAO.findVisibleBookings(
                    user.getUsername(),
                    user.getRole()
            );

            List<BookingResponse> response = new ArrayList<>();

            for (Booking booking : bookings) {
                response.add(new BookingResponse(booking));
            }

            return Response.ok(response).build();

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @POST
    public Response createBooking(BookingRequest request) {
        try {
            CurrentUser user = getCurrentUser();
            Booking booking = toBooking(request);

            Booking created = bookingDAO.createBooking(
                    user.getUsername(),
                    booking
            );

            return Response.status(Response.Status.CREATED)
                    .entity(new BookingResponse(created))
                    .build();

        } catch (DateTimeParseException e) {
            return badRequest("Invalid date or time format. Use YYYY-MM-DD and HH:MM.");

        } catch (InvalidBookingException e) {
            return badRequest(e.getMessage());

        } catch (BookingConflictException e) {
            return conflict(e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateBooking(@PathParam("id") int bookingId,
                                  BookingRequest request) {
        try {
            CurrentUser user = getCurrentUser();
            Booking updatedBooking = toBooking(request);

            Booking updated = bookingDAO.updateBooking(
                    bookingId,
                    updatedBooking,
                    user.getUsername(),
                    user.getRole()
            );

            return Response.ok(new BookingResponse(updated)).build();

        } catch (DateTimeParseException e) {
            return badRequest("Invalid date or time format. Use YYYY-MM-DD and HH:MM.");

        } catch (BookingNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();

        } catch (BookingAccessException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();

        } catch (InvalidBookingException e) {
            return badRequest(e.getMessage());

        } catch (BookingConflictException e) {
            return conflict(e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response cancelBooking(@PathParam("id") int bookingId) {
        try {
            CurrentUser user = getCurrentUser();

            bookingDAO.cancelBooking(
                    bookingId,
                    user.getUsername(),
                    user.getRole()
            );

            return Response.noContent().build();

        } catch (BookingNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();

        } catch (BookingAccessException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();

        } catch (InvalidBookingException e) {
            return badRequest(e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
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

    private CurrentUser getCurrentUser() {
        return (CurrentUser) requestContext.getProperty(AuthFilter.CURRENT_USER_PROPERTY);
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .build();
    }

    private Response conflict(String message) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(message))
                .build();
    }

    private Response serverError() {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Server error."))
                .build();
    }
}