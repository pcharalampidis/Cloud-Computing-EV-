package com.evcharging.dao;

import com.evcharging.db.DatabaseManager;
import com.evcharging.model.User;
import com.evcharging.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    public User findByUsername(String username) throws Exception {
        String sql = "SELECT username, password_hash, role FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

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

    public User authenticate(String username, String password) throws Exception {
        User user = findByUsername(username);

        if (user == null) {
            return null;
        }

        boolean validPassword = PasswordUtil.verifyPassword(
                password,
                user.getPasswordHash()
        );

        if (!validPassword) {
            return null;
        }

        return user;
    }

    public boolean exists(String username) throws Exception {
        String sql = "SELECT username FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}