package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/api/address")
public class AddressServlet extends HttpServlet {
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

    // GET /api/address: lists the logged-in customer's addresses
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        Integer customerId = Auth.currentCustomer(req);
        if (customerId == null) {
            sendError(res, 401, "Please log in");
            return;
        }

        String sql = "SELECT address_id, street, city, pincode, address_type " +
                     "FROM ADDRESS WHERE customer_id = ? ORDER BY address_id";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            JsonArray list = new JsonArray();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject a = new JsonObject();
                    a.addProperty("id", rs.getInt("address_id"));
                    a.addProperty("street", rs.getString("street"));
                    a.addProperty("city", rs.getString("city"));
                    a.addProperty("pincode", rs.getString("pincode"));
                    a.addProperty("address_type", rs.getString("address_type"));
                    list.add(a);
                }
            }
            res.getWriter().print(list.toString());
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    // POST /api/address (form fields: action = add | delete)
    //   add:    street, city, pincode, address_type
    //   delete: address_id
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        Integer customerId = Auth.currentCustomer(req);
        if (customerId == null) {
            sendError(res, 401, "Please log in");
            return;
        }

        String action = req.getParameter("action");
        if ("add".equals(action)) {
            addAddress(req, res, customerId);
        } else if ("delete".equals(action)) {
            deleteAddress(req, res, customerId);
        } else {
            sendError(res, 400, "action must be add or delete");
        }
    }

    private void addAddress(HttpServletRequest req, HttpServletResponse res, int customerId) throws IOException {
        String street = trim(req.getParameter("street"));
        String city = trim(req.getParameter("city"));
        String pincode = trim(req.getParameter("pincode"));
        String type = trim(req.getParameter("address_type"));

        if (street == null || city == null || pincode == null) {
            sendError(res, 400, "street, city and pincode are required");
            return;
        }
        if (type == null) type = "Home";

        String sql = "INSERT INTO ADDRESS (customer_id, street, city, pincode, address_type) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING address_id";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, street);
            ps.setString(3, city);
            ps.setString(4, pincode);
            ps.setString(5, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                JsonObject result = new JsonObject();
                result.addProperty("status", "ok");
                result.addProperty("address_id", rs.getInt(1));
                res.getWriter().print(result.toString());
            }
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    private void deleteAddress(HttpServletRequest req, HttpServletResponse res, int customerId) throws IOException {
        int addressId;
        try {
            addressId = Integer.parseInt(req.getParameter("address_id"));
        } catch (NumberFormatException e) {
            sendError(res, 400, "address_id must be a number");
            return;
        }

        // Only delete if the address belongs to this customer
        String sql = "DELETE FROM ADDRESS WHERE address_id = ? AND customer_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addressId);
            ps.setInt(2, customerId);
            if (ps.executeUpdate() == 0) {
                sendError(res, 404, "Address not found");
                return;
            }
            res.getWriter().print("{\"status\":\"ok\"}");
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    private static String trim(String s) {
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