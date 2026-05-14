package com.evcharging.dao;

import com.evcharging.db.DatabaseManager;
import com.evcharging.model.Station;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StationDAO {

    public List<Station> findAll() throws Exception {
        String sql = "SELECT station_id, name, address, latitude, longitude FROM stations ORDER BY station_id";
        List<Station> stations = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                stations.add(mapStation(rs));
            }
        }

        return stations;
    }

    public Station findById(int stationId) throws Exception {
        String sql = "SELECT station_id, name, address, latitude, longitude FROM stations WHERE station_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapStation(rs);
                }
            }
        }

        return null;
    }

    public Station create(Station station) throws Exception {
        String sql = """
                INSERT INTO stations(name, address, latitude, longitude)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, station.getName());
            stmt.setString(2, station.getAddress());
            stmt.setDouble(3, station.getLatitude());
            stmt.setDouble(4, station.getLongitude());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    station.setStationId(keys.getInt(1));
                }
            }
        }

        return station;
    }

    public boolean update(int stationId, Station station) throws Exception {
        String sql = """
                UPDATE stations
                SET name = ?, address = ?, latitude = ?, longitude = ?
                WHERE station_id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, station.getName());
            stmt.setString(2, station.getAddress());
            stmt.setDouble(3, station.getLatitude());
            stmt.setDouble(4, station.getLongitude());
            stmt.setInt(5, stationId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int stationId) throws Exception {
        String sql = "DELETE FROM stations WHERE station_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stationId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Station mapStation(ResultSet rs) throws Exception {
        return new Station(
                rs.getInt("station_id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude")
        );
    }
}