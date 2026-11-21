package com.scentzone.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {}

    public static Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault("SCENT_DB_URL",
                "jdbc:mysql://localhost:3306/scent_zone_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String username = System.getenv("SCENT_DB_USER");
        String password = System.getenv("SCENT_DB_PASSWORD");
        if (username == null || username.isBlank() || password == null) {
            throw new SQLException("Set SCENT_DB_USER and SCENT_DB_PASSWORD before running Scent Zone.");
        }
        return DriverManager.getConnection(url, username, password);
    }
}
