package com.evcharging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectorRequest {
    @JsonProperty("connector_type")
    private String connectorType;

    public ConnectorRequest() {
    }

    public String getConnectorType() {
        return connectorType;
    }
}