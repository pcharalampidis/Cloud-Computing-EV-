package com.evcharging.resource;

import com.evcharging.dao.ConnectorDAO;
import com.evcharging.dto.ConnectorRequest;
import com.evcharging.dto.ErrorResponse;
import com.evcharging.model.Connector;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectorResource {

    private final ConnectorDAO connectorDAO = new ConnectorDAO();

    @POST
    @Path("/stations/{stationId}/connectors")
    public Response createConnector(@PathParam("stationId") int stationId, ConnectorRequest request) {
        try {
            if (request == null || request.getConnectorType() == null || request.getConnectorType().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Connector type is required."))
                        .build();
            }

            Connector connector = new Connector();
            connector.setConnectorType(request.getConnectorType());

            Connector created = connectorDAO.create(stationId, connector);

            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();

        } catch (SQLException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Station not found."))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @PUT
    @Path("/connectors/{id}")
    public Response updateConnector(@PathParam("id") int id, ConnectorRequest request) {
        try {
            if (request == null || request.getConnectorType() == null || request.getConnectorType().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Connector type is required."))
                        .build();
            }

            Connector connector = new Connector();
            connector.setConnectorType(request.getConnectorType());

            boolean updated = connectorDAO.update(id, connector);

            if (!updated) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Connector not found."))
                        .build();
            }

            return Response.ok(connectorDAO.findById(id)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return serverError();
        }
    }

    @DELETE
    @Path("/connectors/{id}")
    public Response deleteConnector(@PathParam("id") int id) {
        try {
            boolean deleted = connectorDAO.delete(id);

            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Connector not found."))
                        .build();
            }

            return Response.noContent().build();

        } catch (SQLException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Connector cannot be deleted because it has bookings."))
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