package com.evcharging.model;

public class Connector {
    private int connectorId;
    private int stationId;
    private String connectorType;

    public Connector() {
    }

    public Connector(int connectorId, int stationId, String connectorType) {
        this.connectorId = connectorId;
        this.stationId = stationId;
        this.connectorType = connectorType;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(int connectorId) {
        this.connectorId = connectorId;
    }

    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }
}