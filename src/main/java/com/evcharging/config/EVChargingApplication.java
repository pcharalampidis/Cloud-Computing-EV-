package com.evcharging.config;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api")
public class EVChargingApplication extends ResourceConfig {

    public EVChargingApplication() {
        packages("com.evcharging.resource");
        packages("com.evcharging.filter");

        register(JacksonFeature.class);
    }
}