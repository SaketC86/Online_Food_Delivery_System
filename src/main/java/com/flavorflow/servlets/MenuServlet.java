package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/api/menu")
public class MenuServlet extends HttpServlet {
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String idParam = req.getParameter("restaurant_id");
        if (idParam == null) {
            sendError(res, 400, "restaurant_id is required");
            return;
        }

        int restaurantId;
        try {
            restaurantId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            sendError(res, 400, "restaurant_id must be a number");
            return;
        }

        JsonArray items = new JsonArray();
        String sql = "SELECT item_id, item_name, price, category FROM MENU_ITEM " +
                     "WHERE restaurant_id = ? AND availability = TRUE ORDER BY category, item_name";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("id", rs.getInt("item_id"));
                    item.addProperty("name", rs.getString("item_name"));
                    item.addProperty("price", rs.getDouble("price"));
                    item.addProperty("category", rs.getString("category"));
                    items.add(item);
                }
            }
            res.getWriter().print(items.toString());
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