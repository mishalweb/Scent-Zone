package com.scentzone.dao;

import com.scentzone.config.Database;
import com.scentzone.model.Brand;
import com.scentzone.model.Perfume;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PerfumeDAO {
    private static final String SELECT = """
        SELECT p.perfume_id, p.brand_id, b.brand_name, p.perfume_name,
               p.category, p.size_ml, p.price, p.stock_quantity, p.description, p.active
        FROM perfumes p JOIN brands b ON b.brand_id = p.brand_id
        """;

    public List<Perfume> list(boolean includeInactive) throws SQLException {
        String sql = SELECT + (includeInactive ? "" : " WHERE p.active = TRUE")
                + " ORDER BY p.perfume_name";
        List<Perfume> perfumes = new ArrayList<>();
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) perfumes.add(readPerfume(rs));
        }
        return perfumes;
    }

    private Perfume readPerfume(ResultSet rs) throws SQLException {
        return new Perfume(rs.getInt("perfume_id"), rs.getInt("brand_id"),
                rs.getString("brand_name"), rs.getString("perfume_name"),
                rs.getString("category"), rs.getInt("size_ml"), rs.getBigDecimal("price"),
                rs.getInt("stock_quantity"), rs.getString("description"), rs.getBoolean("active"));
    }

    public List<Brand> listBrands() throws SQLException {
        List<Brand> brands = new ArrayList<>();
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement("SELECT brand_id, brand_name FROM brands ORDER BY brand_name");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) brands.add(new Brand(rs.getInt(1), rs.getString(2)));
        }
        return brands;
    }

    public void addBrand(String brandName) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement("INSERT INTO brands(brand_name) VALUES (?)")) {
            stmt.setString(1, brandName);
            stmt.executeUpdate();
        }
    }

    public void add(int brandId, String name, String category, int sizeMl, BigDecimal price,
                    int stock, String description) throws SQLException {
        String sql = """
          INSERT INTO perfumes(brand_id, perfume_name, category, size_ml, price, stock_quantity, description)
          VALUES(?,?,?,?,?,?,?)
          """;
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, brandId, name, category, sizeMl, price, stock, description);
            stmt.executeUpdate();
        }
    }

    public void update(int id, int brandId, String name, String category, int sizeMl,
                       BigDecimal price, int stock, String description) throws SQLException {
        String sql = """
          UPDATE perfumes SET brand_id=?, perfume_name=?, category=?, size_ml=?,
             price=?, stock_quantity=?, description=? WHERE perfume_id=?
          """;
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, brandId, name, category, sizeMl, price, stock, description);
            stmt.setInt(8, id);
            if (stmt.executeUpdate() == 0) throw new SQLException("Perfume no longer exists.");
        }
    }

    private void bind(PreparedStatement stmt, int brandId, String name, String category,
                      int sizeMl, BigDecimal price, int stock, String description) throws SQLException {
        stmt.setInt(1, brandId);
        stmt.setString(2, name);
        stmt.setString(3, category);
        stmt.setInt(4, sizeMl);
        stmt.setBigDecimal(5, price);
        stmt.setInt(6, stock);
        stmt.setString(7, description);
    }

    public void setActive(int id, boolean active) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement("UPDATE perfumes SET active = ? WHERE perfume_id = ?")) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            if (stmt.executeUpdate() == 0) throw new SQLException("Perfume no longer exists.");
        }
    }

    public DashboardStats dashboardStats() throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement("""
               SELECT (SELECT COUNT(*) FROM perfumes WHERE active = TRUE) AS total_perfumes,
                      (SELECT COUNT(*) FROM perfumes WHERE active = TRUE AND stock_quantity < 5) AS low_stock,
                      (SELECT COUNT(*) FROM orders) AS total_orders,
                      (SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE status <> 'CANCELLED') AS revenue
               """);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return new DashboardStats(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4));
        }
    }

    public record DashboardStats(int totalPerfumes, int lowStock, int totalOrders, BigDecimal revenue) {}
}
