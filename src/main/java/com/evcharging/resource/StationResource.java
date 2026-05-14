package com.evcharging.resource;

import com.evcharging.dao.ConnectorDAO;
import com.evcharging.dao.StationDAO;
import com.evcharging.dto.ErrorResponse;
import com.evcharging.dto.StationDetailsResponse;
import com.evcharging.model.Connector;
import com.evcharging.model.Station;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StationResource {

    private final StationDAO stationDAO = new StationDAO();
    private final ConnectorDAO connectorDAO = new ConnectorDAO();

    @GET
    public Response getAllStations() {
        try {
            return Response.ok(stationDAO.findAll()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @GET
    @Path("/{id}")
    public Response getStationById(@PathParam("id") int id) {
        try {
            Station station = stationDAO.findById(id);

            if (station == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Station not found."))
                        .build();
            }

            List<Connector> connectors = connectorDAO.findByStationId(id);
            return Response.ok(new StationDetailsResponse(station, connectors)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @POST
    public Response createStation(Station station) {
        try {
            if (station == null || station.getName() == null || station.getAddress() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Station name and address are required."))
                        .build();
            }

            Station created = stationDAO.create(station);

            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateStation(@PathParam("id") int id, Station station) {
        try {
            boolean updated = stationDAO.update(id, station);

            if (!updated) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Station not found."))
                        .build();
            }

            Station updatedStation = stationDAO.findById(id);
            return Response.ok(updatedStation).build();

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteStation(@PathParam("id") int id) {
        try {
            boolean deleted = stationDAO.delete(id);

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

    private Response serverError() {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Server error."))
                .build();
    }
}