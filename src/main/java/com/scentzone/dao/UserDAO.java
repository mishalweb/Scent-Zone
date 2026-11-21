package com.scentzone.dao;

import com.scentzone.config.Database;
import com.scentzone.model.User;
import java.sql.*;
import java.util.Optional;

public class UserDAO {
    public record UserCredentials(User user, String passwordHash) {}

    public Optional<UserCredentials> findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, full_name, email, role, password_hash FROM users WHERE email = ?";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new UserCredentials(
                        new User(rs.getInt("user_id"), rs.getString("full_name"),
                                rs.getString("email"), rs.getString("role")),
                        rs.getString("password_hash")));
            }
        }
    }

    public void create(String name, String email, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,?)";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.executeUpdate();
        }
    }
}
