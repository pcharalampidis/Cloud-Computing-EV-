package com.evcharging.db;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseManager {

    public static Connection getConnection() throws Exception {
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        String username = System.getenv("JDBC_DATABASE_USERNAME");
        String password = System.getenv("JDBC_DATABASE_PASSWORD");

        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            HerokuDbConfig config = parseHerokuDatabaseUrl(databaseUrl);
            return DriverManager.getConnection(
                    config.jdbcUrl,
                    config.username,
                    config.password
            );
        }

        throw new IllegalStateException(
                "Database environment variables are missing. " +
                "Set JDBC_DATABASE_URL, JDBC_DATABASE_USERNAME, and JDBC_DATABASE_PASSWORD."
        );
    }

    private static HerokuDbConfig parseHerokuDatabaseUrl(String databaseUrl) throws Exception {
        URI uri = new URI(databaseUrl);

        String[] userInfo = uri.getUserInfo().split(":", 2);

        String username = URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8);
        String password = URLDecoder.decode(userInfo[1], StandardCharsets.UTF_8);

        String host = uri.getHost();
        int port = uri.getPort();
        String databaseName = uri.getPath();

        String jdbcUrl = "jdbc:postgresql://" + host;

        if (port != -1) {
            jdbcUrl += ":" + port;
        }

        jdbcUrl += databaseName;

        if (uri.getQuery() == null || uri.getQuery().isBlank()) {
            jdbcUrl += "?sslmode=require";
        } else {
            jdbcUrl += "?" + uri.getQuery() + "&sslmode=require";
        }

        return new HerokuDbConfig(jdbcUrl, username, password);
    }

    private static class HerokuDbConfig {
        private final String jdbcUrl;
        private final String username;
        private final String password;

        private HerokuDbConfig(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}