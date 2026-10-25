package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

@WebServlet("/api/tracking")
public class TrackingServlet extends HttpServlet {
    static final String URL = "jdbc:postgresql://localhost:5432/food_delivery";
    static final String USER = "postgres";
    static final String PASS = "YOUR_PASSWORD";

    // No login yet, so only customer 1's orders can be tracked
    static final int CUSTOMER_ID = 1;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // GET /api/tracking?order_id=5
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

        String orderSql = "SELECT o.order_id, o.order_date, o.status, o.total_amount, o.agent_id, " +
                          "       r.name AS restaurant_name, " +
                          "       a.name AS agent_name, a.phone AS agent_phone, a.vehicle_no " +
                          "FROM ORDERS o " +
                          "JOIN RESTAURANT r ON r.restaurant_id = o.restaurant_id " +
                          "LEFT JOIN DELIVERY_AGENT a ON a.agent_id = o.agent_id " +
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
                    result.addProperty("order_date", rs.getTimestamp("order_date").toLocalDateTime().toString());
                    result.addProperty("status", rs.getString("status"));
                    result.addProperty("total", rs.getDouble("total_amount"));
                    result.addProperty("restaurant_name", rs.getString("restaurant_name"));

                    // Driver is null until an agent is assigned
                    if (rs.getObject("agent_id") == null) {
                        result.add("agent", JsonNull.INSTANCE);
                    } else {
                        JsonObject agent = new JsonObject();
                        agent.addProperty("name", rs.getString("agent_name"));
                        agent.addProperty("phone", rs.getString("agent_phone"));
                        agent.addProperty("vehicle_no", rs.getString("vehicle_no"));
                        result.add("agent", agent);
                    }
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