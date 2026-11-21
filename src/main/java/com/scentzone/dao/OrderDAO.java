package com.scentzone.dao;

import com.scentzone.config.Database;
import com.scentzone.model.CartLine;
import com.scentzone.model.OrderLine;
import com.scentzone.model.OrderSummary;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class OrderDAO {
    public int checkout(int userId, List<CartLine> cart) throws SQLException {
        if (cart == null || cart.isEmpty()) throw new SQLException("Your cart is empty.");
        // Lock inventory rows in deterministic order, and always calculate from database prices.
        List<CartLine> sorted = new ArrayList<>(cart);
        sorted.sort(Comparator.comparingInt(c -> c.getPerfume().id()));
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                List<CheckedLine> checked = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                for (CartLine line : sorted) {
                    if (line.getQuantity() < 1) throw new SQLException("Invalid quantity.");
                    String sql = "SELECT perfume_name, price, stock_quantity, active FROM perfumes WHERE perfume_id=? FOR UPDATE";
                    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                        stmt.setInt(1, line.getPerfume().id());
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next() || !rs.getBoolean("active"))
                                throw new SQLException("A perfume in your cart is no longer available.");
                            if (rs.getInt("stock_quantity") < line.getQuantity())
                                throw new SQLException("Not enough stock for " + rs.getString("perfume_name") + ".");
                            BigDecimal price = rs.getBigDecimal("price");
                            checked.add(new CheckedLine(line.getPerfume().id(), line.getQuantity(), price));
                            total = total.add(price.multiply(BigDecimal.valueOf(line.getQuantity())));
                        }
                    }
                }

                int orderId;
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO orders(user_id,total_amount,status) VALUES(?,?,'PENDING')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, userId);
                    stmt.setBigDecimal(2, total);
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create order.");
                        orderId = keys.getInt(1);
                    }
                }
                for (CheckedLine item : checked) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO order_items(order_id,perfume_id,quantity,unit_price) VALUES(?,?,?,?)")) {
                        stmt.setInt(1, orderId);
                        stmt.setInt(2, item.perfumeId());
                        stmt.setInt(3, item.quantity());
                        stmt.setBigDecimal(4, item.price());
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "UPDATE perfumes SET stock_quantity = stock_quantity - ? WHERE perfume_id = ? AND stock_quantity >= ?")) {
                        stmt.setInt(1, item.quantity());
                        stmt.setInt(2, item.perfumeId());
                        stmt.setInt(3, item.quantity());
                        if (stmt.executeUpdate() != 1) throw new SQLException("Stock changed during checkout.");
                    }
                }
                connection.commit();
                return orderId;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<OrderSummary> listOrders(Integer userId) throws SQLException {
        String sql = """
          SELECT o.order_id, u.full_name, o.total_amount, o.status, o.order_date,
                 COALESCE(SUM(oi.quantity), 0) item_count
          FROM orders o JOIN users u ON u.user_id=o.user_id
          LEFT JOIN order_items oi ON oi.order_id=o.order_id
          """ + (userId == null ? "" : " WHERE o.user_id=? ")
                + " GROUP BY o.order_id, u.full_name, o.total_amount, o.status, o.order_date ORDER BY o.order_date DESC, o.order_id DESC";
        List<OrderSummary> rows = new ArrayList<>();
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (userId != null) stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OrderSummary(rs.getInt("order_id"), rs.getString("full_name"),
                            rs.getBigDecimal("total_amount"), rs.getString("status"),
                            rs.getTimestamp("order_date").toLocalDateTime(), rs.getInt("item_count")));
                }
            }
        }
        return rows;
    }

    public List<OrderLine> getOrderItems(int orderId) throws SQLException {
        String sql = """
          SELECT p.perfume_name, oi.quantity, oi.unit_price
          FROM order_items oi JOIN perfumes p ON p.perfume_id = oi.perfume_id
          WHERE oi.order_id=? ORDER BY oi.order_item_id
          """;
        List<OrderLine> rows = new ArrayList<>();
        try (Connection connection = Database.connect();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) rows.add(new OrderLine(rs.getString(1), rs.getInt(2), rs.getBigDecimal(3)));
            }
        }
        return rows;
    }

    public void updateStatus(int orderId, String newStatus) throws SQLException {
        Map<String, Set<String>> allowed = Map.of(
                "PENDING", Set.of("CONFIRMED", "CANCELLED"),
                "CONFIRMED", Set.of("SHIPPED", "CANCELLED"),
                "SHIPPED", Set.of("DELIVERED"),
                "DELIVERED", Set.of(),
                "CANCELLED", Set.of());
        if (!allowed.containsKey(newStatus)) throw new SQLException("Invalid order status.");
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                String old;
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT status FROM orders WHERE order_id=? FOR UPDATE")) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Order not found.");
                        old = rs.getString(1);
                    }
                }
                if (!allowed.getOrDefault(old, Set.of()).contains(newStatus))
                    throw new SQLException("Cannot change status from " + old + " to " + newStatus + ".");
                if ("CANCELLED".equals(newStatus)) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT perfume_id, quantity FROM order_items WHERE order_id=?")) {
                        stmt.setInt(1, orderId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                try (PreparedStatement restock = connection.prepareStatement(
                                        "UPDATE perfumes SET stock_quantity=stock_quantity+? WHERE perfume_id=?")) {
                                    restock.setInt(1, rs.getInt("quantity"));
                                    restock.setInt(2, rs.getInt("perfume_id"));
                                    restock.executeUpdate();
                                }
                            }
                        }
                    }
                }
                try (PreparedStatement stmt = connection.prepareStatement("UPDATE orders SET status=? WHERE order_id=?")) {
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, orderId);
                    stmt.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private record CheckedLine(int perfumeId, int quantity, BigDecimal price) {}
}
