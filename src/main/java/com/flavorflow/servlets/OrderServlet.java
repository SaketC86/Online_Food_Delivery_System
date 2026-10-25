package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/api/order")
public class OrderServlet extends HttpServlet {
    static final String URL = "jdbc:postgresql://localhost:5432/food_delivery";
    static final String USER = "postgres";
    static final String PASS = "YOUR_PASSWORD";


    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // GET /api/order?order_id=5
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        Integer customerId = Auth.currentCustomer(req);
if (customerId == null) {
    sendError(res, 401, "Please log in");
    return;
}

        int orderId;
        try {
            orderId = Integer.parseInt(req.getParameter("order_id"));
        } catch (NumberFormatException e) {
            sendError(res, 400, "order_id must be a number");
            return;
        }

        String orderSql = "SELECT o.order_id, o.order_date, o.status, o.total_amount, " +
                          "       r.name AS restaurant_name, p.method, p.status AS payment_status " +
                          "FROM ORDERS o " +
                          "JOIN RESTAURANT r ON r.restaurant_id = o.restaurant_id " +
                          "LEFT JOIN PAYMENT p ON p.order_id = o.order_id " +
                          "WHERE o.order_id = ? AND o.customer_id = ?";

        String itemSql = "SELECT m.item_name, oi.quantity, oi.unit_price " +
                         "FROM ORDER_ITEM oi " +
                         "JOIN MENU_ITEM m ON m.item_id = oi.item_id " +
                         "WHERE oi.order_id = ? " +
                         "ORDER BY m.item_name";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            JsonObject result = new JsonObject();
            Dispatcher.run(conn);

            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        sendError(res, 404, "Order not found");
                        return;
                    }
                    result.addProperty("order_id", rs.getInt("order_id"));
                    result.addProperty("order_date", String.valueOf(rs.getTimestamp("order_date")));
                    result.addProperty("status", rs.getString("status"));
                    result.addProperty("total", rs.getDouble("total_amount"));
                    result.addProperty("restaurant_name", rs.getString("restaurant_name"));
                    result.addProperty("payment_method", rs.getString("method"));
                    result.addProperty("payment_status", rs.getString("payment_status"));
                }
            }

            JsonArray items = new JsonArray();
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JsonObject item = new JsonObject();
                        item.addProperty("name", rs.getString("item_name"));
                        item.addProperty("quantity", rs.getInt("quantity"));
                        item.addProperty("unit_price", rs.getDouble("unit_price"));
                        items.add(item);
                    }
                }
            }
            result.add("items", items);

            res.getWriter().print(result.toString());
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    private void sendError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        res.getWriter().print(error.toString());
    }
}