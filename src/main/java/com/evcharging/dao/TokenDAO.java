package com.evcharging.dao;

import com.evcharging.db.DatabaseManager;
import com.evcharging.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class TokenDAO {

    public String createToken(String username) throws Exception {
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);

        String sql = """
                INSERT INTO auth_tokens(token, username, created_at, expires_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            stmt.setString(2, username);
            stmt.setTimestamp(3, Timestamp.valueOf(now));
            stmt.setTimestamp(4, Timestamp.valueOf(expiresAt));

            stmt.executeUpdate();
        }

        return token;
    }

    public User findUserByToken(String token) throws Exception {
        String sql = """
                SELECT u.username, u.password_hash, u.role
                FROM auth_tokens t
                JOIN users u ON t.username = u.username
                WHERE t.token = ?
                AND t.expires_at > CURRENT_TIMESTAMP
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("role")
                    );
                }
            }
        }

        return null;
    }

    public void deleteToken(String token) throws Exception {
        String sql = "DELETE FROM auth_tokens WHERE token = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            stmt.executeUpdate();
        }
    }

    public void deleteExpiredTokens() throws Exception {
        String sql = "DELETE FROM auth_tokens WHERE expires_at <= CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.executeUpdate();
        }
    }
}