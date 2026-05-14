package com.evcharging.filter;

import com.evcharging.dao.TokenDAO;
import com.evcharging.dto.ErrorResponse;
import com.evcharging.model.User;
import com.evcharging.security.CurrentUser;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    public static final String CURRENT_USER_PROPERTY = "currentUser";

    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();

        if (isPublicPath(path)) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            abortUnauthorized(requestContext, "Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        if (token.isBlank()) {
            abortUnauthorized(requestContext, "Missing token.");
            return;
        }

        try {
            User user = tokenDAO.findUserByToken(token);

            if (user == null) {
                abortUnauthorized(requestContext, "Invalid or expired token.");
                return;
            }

            CurrentUser currentUser = new CurrentUser(
                    user.getUsername(),
                    user.getRole()
            );

            requestContext.setProperty(CURRENT_USER_PROPERTY, currentUser);

            if (!isAllowed(requestContext, currentUser)) {
                requestContext.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                                .entity(new ErrorResponse("You do not have permission to access this resource."))
                                .build()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();

            requestContext.abortWith(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse("Authentication check failed."))
                            .build()
            );
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("auth/login") || path.equals("health");
    }

    private boolean isAllowed(ContainerRequestContext requestContext, CurrentUser user) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        if (user.isAdmin()) {
            return true;
        }

        if (user.isDriver()) {
            if (path.startsWith("stations") && method.equals("GET")) {
                return true;
            }

            if (path.startsWith("connectors") && method.equals("GET")) {
                return true;
            }

            if (path.startsWith("bookings")) {
                return method.equals("GET")
                        || method.equals("POST")
                        || method.equals("PUT")
                        || method.equals("DELETE");
            }
        }

        return false;
    }

    private void abortUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse(message))
                        .build()
        );
    }
}