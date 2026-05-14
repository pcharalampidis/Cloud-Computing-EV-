package com.evcharging.filter;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.time.LocalDateTime;

@Provider
@Priority(Priorities.USER)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME_PROPERTY = "requestStartTime";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        long startTime = System.currentTimeMillis();
        requestContext.setProperty(START_TIME_PROPERTY, startTime);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {

        Object startObject = requestContext.getProperty(START_TIME_PROPERTY);

        long startTime = startObject instanceof Long
                ? (Long) startObject
                : System.currentTimeMillis();

        long processingTime = System.currentTimeMillis() - startTime;

        String timestamp = LocalDateTime.now().toString();
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        int status = responseContext.getStatus();

        String dyno = System.getenv("DYNO");

        if (dyno == null || dyno.isBlank()) {
            dyno = "local";
        }

        System.out.println(
                "timestamp=" + timestamp +
                " method=" + method +
                " uri=" + uri +
                " status=" + status +
                " processingTimeMs=" + processingTime +
                " instance=" + dyno
        );
    }
}