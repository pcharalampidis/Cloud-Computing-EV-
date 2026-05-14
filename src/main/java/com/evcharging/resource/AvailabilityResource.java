package com.evcharging.resource;

import com.evcharging.dto.ErrorResponse;
import com.evcharging.service.AvailabilityService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

@Path("/connectors/{connectorId}/availability")
@Produces(MediaType.APPLICATION_JSON)
public class AvailabilityResource {

    private final AvailabilityService availabilityService = new AvailabilityService();

    @GET
    public Response getAvailability(
            @PathParam("connectorId") int connectorId,
            @QueryParam("date") String dateText) {

        try {
            return Response.ok(
                    availabilityService.getAvailableSlots(connectorId, dateText)
            ).build();

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (DateTimeParseException e) {
            return badRequest("Invalid date format. Use YYYY-MM-DD.");
        } catch (NoSuchElementException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Server error while checking availability."))
                    .build();
        }
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .build();
    }
}