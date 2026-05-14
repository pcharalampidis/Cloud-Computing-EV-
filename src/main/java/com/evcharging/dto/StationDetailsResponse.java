package com.evcharging.dto;

import com.evcharging.model.Connector;
import com.evcharging.model.Station;

import java.util.List;

public class StationDetailsResponse {
    private Station station;
    private List<Connector> connectors;

    public StationDetailsResponse() {
    }

    public StationDetailsResponse(Station station, List<Connector> connectors) {
        this.station = station;
        this.connectors = connectors;
    }

    public Station getStation() {
        return station;
    }

    public List<Connector> getConnectors() {
        return connectors;
    }
}