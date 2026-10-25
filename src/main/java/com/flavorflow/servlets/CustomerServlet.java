package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/api/customer")
public class CustomerServlet extends HttpServlet {
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

    // GET /api/customer: returns the profile and the 5 most recent orders
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        Integer customerId = Auth.currentCustomer(req);
if (customerId == null) {
    sendError(res, 401, "Please log in");
    return;
}

        String customerSql = "SELECT name, email, phone FROM CUSTOMER WHERE customer_id = ?";
        String orderSql = "SELECT o.order_id, o.order_date, o.status, o.total_amount, r.name AS restaurant_name " +
                          "FROM ORDERS o JOIN RESTAURANT r ON r.restaurant_id = o.restaurant_id " +
                          "WHERE o.customer_id = ? " +
                          "ORDER BY o.order_date DESC, o.order_id DESC LIMIT 5";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            JsonObject result = new JsonObject();
            Dispatcher.run(conn);

            try (PreparedStatement ps = conn.prepareStatement(customerSql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        sendError(res, 404, "Customer not found");
                        return;
                    }
                    result.addProperty("name", rs.getString("name"));
                    result.addProperty("email", rs.getString("email"));
                    result.addProperty("phone", rs.getString("phone"));
                }
            }

            JsonArray orders = new JsonArray();
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("order_id", rs.getInt("order_id"));
                        o.addProperty("order_date", rs.getTimestamp("order_date").toLocalDateTime().toString());
                        o.addProperty("status", rs.getString("status"));
                        o.addProperty("total", rs.getDouble("total_amount"));
                        o.addProperty("restaurant_name", rs.getString("restaurant_name"));
                        orders.add(o);
                    }
                }
            }
            result.add("orders", orders);

            res.getWriter().print(result.toString());
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }
        // POST /api/customer (form fields: name, email, phone)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        Integer customerId = Auth.currentCustomer(req);
        if (customerId == null) {
            sendError(res, 401, "Please log in");
            return;
        }

        String name = trimOrNull(req.getParameter("name"));
        String email = trimOrNull(req.getParameter("email"));
        String phone = trimOrNull(req.getParameter("phone"));

        if (name == null || email == null || phone == null) {
            sendError(res, 400, "name, email and phone are required");
            return;
        }
        if (!phone.matches("\\d{10}")) {
            sendError(res, 400, "phone must be exactly 10 digits");
            return;
        }
        if (!email.contains("@")) {
            sendError(res, 400, "enter a valid email address");
            return;
        }

        String sql = "UPDATE CUSTOMER SET name = ?, email = ?, phone = ? WHERE customer_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setInt(4, customerId);
            ps.executeUpdate();
            res.getWriter().print("{\"status\":\"ok\"}");
        } catch (SQLException e) {
            // 23505 means the email is already used by another customer
            if ("23505".equals(e.getSQLState())) {
                sendError(res, 400, "That email is already used by another account");
            } else {
                sendError(res, 500, e.getMessage());
            }
        }
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private void sendError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        res.getWriter().print(error.toString());
    }
}