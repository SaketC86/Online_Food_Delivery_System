package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonObject;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {
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

    // POST /api/register (form fields: name, email, phone, password)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String name = trimOrNull(req.getParameter("name"));
        String email = trimOrNull(req.getParameter("email"));
        String phone = trimOrNull(req.getParameter("phone"));
        String password = req.getParameter("password");

        if (name == null || email == null || phone == null || password == null || password.isEmpty()) {
            sendError(res, 400, "Please fill in all fields");
            return;
        }
        if (!email.contains("@")) {
            sendError(res, 400, "Enter a valid email address");
            return;
        }
        if (!phone.matches("\\d{10}")) {
            sendError(res, 400, "Phone must be exactly 10 digits");
            return;
        }
        if (password.length() < 6) {
            sendError(res, 400, "Password must be at least 6 characters");
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);
            try {
                int customerId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO CUSTOMER (name, email, phone, password) VALUES (?, ?, ?, ?) RETURNING customer_id")) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, phone);
                    ps.setString(4, password);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        customerId = rs.getInt(1);
                    }
                }

                // Every customer needs a cart, so create one now
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO CART (customer_id) VALUES (?)")) {
                    ps.setInt(1, customerId);
                    ps.executeUpdate();
                }

                conn.commit();

                // Log the new customer in straight away
                HttpSession session = req.getSession(true);
                session.setAttribute(Auth.SESSION_KEY, customerId);

                JsonObject result = new JsonObject();
                result.addProperty("status", "ok");
                result.addProperty("customer_id", customerId);
                res.getWriter().print(result.toString());
            } catch (SQLException e) {
                conn.rollback();
                // 23505 means the email is already registered
                if ("23505".equals(e.getSQLState())) {
                    sendError(res, 400, "An account with this email already exists");
                } else {
                    sendError(res, 500, e.getMessage());
                }
            }
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
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