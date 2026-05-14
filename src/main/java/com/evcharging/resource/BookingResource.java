package com.evcharging.resource;

import com.evcharging.dto.BookingRequest;
import com.evcharging.dto.ErrorResponse;
import com.evcharging.exception.BookingAccessException;
import com.evcharging.exception.BookingConflictException;
import com.evcharging.exception.BookingNotFoundException;
import com.evcharging.exception.InvalidBookingException;
import com.evcharging.filter.AuthFilter;
import com.evcharging.security.CurrentUser;
import com.evcharging.service.BookingService;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.format.DateTimeParseException;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    private final BookingService bookingService = new BookingService();

    @Context
    private ContainerRequestContext requestContext;

    @GET
    public Response getBookings() {
        try {
            CurrentUser user = getCurrentUser();
            return Response.ok(bookingService.getVisibleBookings(user)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @POST
    public Response createBooking(BookingRequest request) {
        try {
            CurrentUser user = getCurrentUser();

            return Response.status(Response.Status.CREATED)
                    .entity(bookingService.createBooking(user, request))
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
    public Response updateBooking(@PathParam("id") int bookingId, BookingRequest request) {
        try {
            CurrentUser user = getCurrentUser();

            return Response.ok(
                    bookingService.updateBooking(bookingId, user, request)
            ).build();

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
            bookingService.cancelBooking(bookingId, user);

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