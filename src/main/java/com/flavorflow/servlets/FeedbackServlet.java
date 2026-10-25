package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonObject;

@WebServlet("/api/feedback")
public class FeedbackServlet extends HttpServlet {
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

    // GET /api/feedback?order_id=5: tells the page whether a review is allowed
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

        String sql = "SELECT o.status, f.rating, f.comments, f.feedback_id " +
                     "FROM ORDERS o LEFT JOIN FEEDBACK f ON f.order_id = o.order_id " +
                     "WHERE o.order_id = ? AND o.customer_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    sendError(res, 404, "Order not found");
                    return;
                }
                String status = rs.getString("status");
                boolean reviewed = rs.getObject("feedback_id") != null;

                JsonObject result = new JsonObject();
                result.addProperty("order_id", orderId);
                result.addProperty("status", status);
                result.addProperty("already_reviewed", reviewed);
                result.addProperty("can_review", "Delivered".equals(status) && !reviewed);
                if (reviewed) {
                    result.addProperty("rating", rs.getInt("rating"));
                    result.addProperty("comments", rs.getString("comments"));
                }
                res.getWriter().print(result.toString());
            }
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    // POST /api/feedback (form fields: order_id, rating 1-5, comments optional)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        Integer customerId = Auth.currentCustomer(req);
        if (customerId == null) {
            sendError(res, 401, "Please log in");
            return;
        }

        int orderId;
        int rating;
        try {
            orderId = Integer.parseInt(req.getParameter("order_id"));
            rating = Integer.parseInt(req.getParameter("rating"));
        } catch (NumberFormatException e) {
            sendError(res, 400, "order_id and rating must be numbers");
            return;
        }
        if (rating < 1 || rating > 5) {
            sendError(res, 400, "rating must be between 1 and 5");
            return;
        }
        String comments = req.getParameter("comments");
        if (comments != null) comments = comments.trim();
        if (comments != null && comments.isEmpty()) comments = null;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);
            try {
                // 1. The order must belong to this customer and be delivered
                String status = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status FROM ORDERS WHERE order_id = ? AND customer_id = ?")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) status = rs.getString("status");
                    }
                }
                if (status == null) {
                    sendError(res, 404, "Order not found");
                    return;
                }
                if (!"Delivered".equals(status)) {
                    sendError(res, 400, "Feedback can only be left after delivery");
                    return;
                }

                // 2. Only one feedback per order
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM FEEDBACK WHERE order_id = ?")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            sendError(res, 400, "You have already reviewed this order");
                            return;
                        }
                    }
                }

                // 3. Save the review
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO FEEDBACK (order_id, customer_id, rating, comments) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, customerId);
                    ps.setInt(3, rating);
                    if (comments == null) ps.setNull(4, Types.VARCHAR);
                    else ps.setString(4, comments);
                    ps.executeUpdate();
                }

                // 4. Restaurant rating = average of all its reviews
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE RESTAURANT SET rating = (" +
                        "  SELECT ROUND(AVG(f.rating), 1) FROM FEEDBACK f " +
                        "  JOIN ORDERS o ON o.order_id = f.order_id " +
                        "  WHERE o.restaurant_id = RESTAURANT.restaurant_id) " +
                        "WHERE restaurant_id = (SELECT restaurant_id FROM ORDERS WHERE order_id = ?)")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }

                conn.commit();
                res.getWriter().print("{\"status\":\"ok\"}");
            } catch (SQLException e) {
                conn.rollback();
                sendError(res, 500, e.getMessage());
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