package com.evcharging.service;

import com.evcharging.dao.ConnectorDAO;
import com.evcharging.dao.StationDAO;
import com.evcharging.dto.StationDetailsResponse;
import com.evcharging.model.Connector;
import com.evcharging.model.Station;

import java.util.List;

public class StationService {

    private final StationDAO stationDAO = new StationDAO();
    private final ConnectorDAO connectorDAO = new ConnectorDAO();

    public List<Station> getAllStations() throws Exception {
        return stationDAO.findAll();
    }

    public StationDetailsResponse getStationDetails(int stationId) throws Exception {
        Station station = stationDAO.findById(stationId);

        if (station == null) {
            return null;
        }

        List<Connector> connectors = connectorDAO.findByStationId(stationId);

        return new StationDetailsResponse(station, connectors);
    }

    public Station createStation(Station station) throws Exception {
        validateStation(station);
        return stationDAO.create(station);
    }

    public Station updateStation(int stationId, Station station) throws Exception {
        validateStation(station);

        boolean updated = stationDAO.update(stationId, station);

        if (!updated) {
            return null;
        }

        return stationDAO.findById(stationId);
    }

    public boolean deleteStation(int stationId) throws Exception {
        return stationDAO.delete(stationId);
    }

    private void validateStation(Station station) {
        if (station == null ||
                station.getName() == null ||
                station.getName().isBlank() ||
                station.getAddress() == null ||
                station.getAddress().isBlank()) {
            throw new IllegalArgumentException("Station name and address are required.");
        }
    }
}