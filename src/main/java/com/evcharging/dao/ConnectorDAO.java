package com.evcharging.dao;

import com.evcharging.db.DatabaseManager;
import com.evcharging.model.Connector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConnectorDAO {

    public List<Connector> findByStationId(int stationId) throws Exception {
        String sql = """
                SELECT connector_id, station_id, connector_type
                FROM connectors
                WHERE station_id = ?
                ORDER BY connector_id
                """;

        List<Connector> connectors = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stationId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    connectors.add(mapConnector(rs));
                }
            }
        }

        return connectors;
    }

    public Connector findById(int connectorId) throws Exception {
        String sql = """
                SELECT connector_id, station_id, connector_type
                FROM connectors
                WHERE connector_id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, connectorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapConnector(rs);
                }
            }
        }

        return null;
    }

    public Connector create(int stationId, Connector connector) throws Exception {
        String sql = """
                INSERT INTO connectors(station_id, connector_type)
                VALUES (?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, stationId);
            stmt.setString(2, connector.getConnectorType());

            stmt.executeUpdate();

            connector.setStationId(stationId);

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    connector.setConnectorId(keys.getInt(1));
                }
            }
        }

        return connector;
    }

    public boolean update(int connectorId, Connector connector) throws Exception {
        String sql = """
                UPDATE connectors
                SET connector_type = ?
                WHERE connector_id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, connector.getConnectorType());
            stmt.setInt(2, connectorId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int connectorId) throws Exception {
        String sql = "DELETE FROM connectors WHERE connector_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, connectorId);
            return stmt.executeUpdate() > 0;
        }
    }

    public Integer findStationIdForConnector(int connectorId) throws Exception {
        String sql = "SELECT station_id FROM connectors WHERE connector_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, connectorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("station_id");
                }
            }
        }

        return null;
    }

    private Connector mapConnector(ResultSet rs) throws Exception {
        return new Connector(
                rs.getInt("connector_id"),
                rs.getInt("station_id"),
                rs.getString("connector_type")
        );
    }
}