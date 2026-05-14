package com.evcharging.service;

import com.evcharging.dao.ConnectorDAO;
import com.evcharging.dto.ConnectorRequest;
import com.evcharging.model.Connector;

public class ConnectorService {

    private final ConnectorDAO connectorDAO = new ConnectorDAO();

    public Connector createConnector(int stationId, ConnectorRequest request) throws Exception {
        validateConnectorRequest(request);

        Connector connector = new Connector();
        connector.setConnectorType(request.getConnectorType());

        return connectorDAO.create(stationId, connector);
    }

    public Connector updateConnector(int connectorId, ConnectorRequest request) throws Exception {
        validateConnectorRequest(request);

        Connector connector = new Connector();
        connector.setConnectorType(request.getConnectorType());

        boolean updated = connectorDAO.update(connectorId, connector);

        if (!updated) {
            return null;
        }

        return connectorDAO.findById(connectorId);
    }

    public boolean deleteConnector(int connectorId) throws Exception {
        return connectorDAO.delete(connectorId);
    }

    private void validateConnectorRequest(ConnectorRequest request) {
        if (request == null ||
                request.getConnectorType() == null ||
                request.getConnectorType().isBlank()) {
            throw new IllegalArgumentException("Connector type is required.");
        }
    }
}