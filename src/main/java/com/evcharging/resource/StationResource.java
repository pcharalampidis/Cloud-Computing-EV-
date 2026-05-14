package com.evcharging.resource;

import com.evcharging.dto.ErrorResponse;
import com.evcharging.dto.StationDetailsResponse;
import com.evcharging.model.Station;
import com.evcharging.service.StationService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StationResource {

    private final StationService stationService = new StationService();

    @GET
    public Response getAllStations() {
        try {
            return Response.ok(stationService.getAllStations()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @GET
    @Path("/{id}")
    public Response getStationById(@PathParam("id") int id) {
        try {
            StationDetailsResponse response = stationService.getStationDetails(id);

            if (response == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Station not found."))
                        .build();
            }

            return Response.ok(response).build();

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @POST
    public Response createStation(Station station) {
        try {
            Station created = stationService.createStation(station);

            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateStation(@PathParam("id") int id, Station station) {
        try {
            Station updated = stationService.updateStation(id, station);

            if (updated == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Station not found."))
                        .build();
            }

            return Response.ok(updated).build();

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteStation(@PathParam("id") int id) {
        try {
            boolean deleted = stationService.deleteStation(id);

            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Station not found."))
                        .build();
            }

            return Response.noContent().build();

        } catch (SQLException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Station cannot be deleted because it has related data."))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .build();
    }

    private Response serverError() {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Server error."))
                .build();
    }
}