package com.evcharging.resource;

import com.evcharging.dao.TokenDAO;
import com.evcharging.dao.UserDAO;
import com.evcharging.dto.ErrorResponse;
import com.evcharging.dto.LoginRequest;
import com.evcharging.dto.LoginResponse;
import com.evcharging.model.User;

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

    private final UserDAO userDAO = new UserDAO();
    private final TokenDAO tokenDAO = new TokenDAO();

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            if (request == null ||
                    request.getUsername() == null ||
                    request.getPassword() == null ||
                    request.getUsername().isBlank() ||
                    request.getPassword().isBlank()) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Username and password are required."))
                        .build();
            }

            User user = userDAO.authenticate(
                    request.getUsername(),
                    request.getPassword()
            );

            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Invalid username or password."))
                        .build();
            }

            String token = tokenDAO.createToken(user.getUsername());

            LoginResponse response = new LoginResponse(
                    token,
                    user.getUsername(),
                    user.getRole()
            );

            return Response.ok(response).build();

        } catch (Exception e) {
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Login failed because of a server error."))
                    .build();
        }
    }
}