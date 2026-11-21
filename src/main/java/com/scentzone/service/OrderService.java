package com.scentzone.service;

import com.scentzone.dao.OrderDAO;
import com.scentzone.model.CartLine;
import java.sql.SQLException;
import java.util.List;

public class OrderService {
    private final OrderDAO dao = new OrderDAO();
    public int placeOrder(int userId, List<CartLine> lines) throws SQLException {
        return dao.checkout(userId, lines);
    }
}
