package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonObject;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {
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

    // POST /api/login (form fields: email, phone, password)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        String password = req.getParameter("password");

        if (email == null || phone == null || password == null
                || email.isBlank() || phone.isBlank() || password.isEmpty()) {
            sendError(res, 400, "Please fill in all fields");
            return;
        }

        String sql = "SELECT customer_id FROM CUSTOMER WHERE email = ? AND phone = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, phone.trim());
            ps.setString(3, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    sendError(res, 401, "Email, phone, or password is incorrect");
                    return;
                }

                int customerId = rs.getInt("customer_id");
                HttpSession session = req.getSession(true);
                session.setAttribute(Auth.SESSION_KEY, customerId);

                JsonObject result = new JsonObject();
                result.addProperty("status", "ok");
                result.addProperty("customer_id", customerId);
                res.getWriter().print(result.toString());
            }
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