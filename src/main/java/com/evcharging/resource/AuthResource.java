package com.evcharging.resource;

import com.evcharging.dto.ErrorResponse;
import com.evcharging.dto.LoginRequest;
import com.evcharging.dto.LoginResponse;
import com.evcharging.service.AuthService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService = new AuthService();

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);

            if (response == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Invalid username or password."))
                        .build();
            }

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Login failed because of a server error."))
                    .build();
        }
    }
}