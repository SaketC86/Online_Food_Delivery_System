package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/api/cart")
public class CartServlet extends HttpServlet {
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

    // GET /api/cart: returns the cart items, subtotal, and item count
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        Integer customerId = Auth.currentCustomer(req);
if (customerId == null) {
    sendError(res, 401, "Please log in");
    return;
}

        String sql = "SELECT m.item_id, m.item_name, m.price, ci.quantity " +
                     "FROM CART c " +
                     "JOIN CART_ITEM ci ON ci.cart_id = c.cart_id " +
                     "JOIN MENU_ITEM m ON m.item_id = ci.item_id " +
                     "WHERE c.customer_id = ? " +
                     "ORDER BY m.item_name";

        JsonArray items = new JsonArray();
        double subtotal = 0;
        int itemCount = 0;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double price = rs.getDouble("price");
                    int qty = rs.getInt("quantity");
                    subtotal += price * qty;
                    itemCount += qty;

                    JsonObject item = new JsonObject();
                    item.addProperty("id", rs.getInt("item_id"));
                    item.addProperty("name", rs.getString("item_name"));
                    item.addProperty("price", price);
                    item.addProperty("quantity", qty);
                    items.add(item);
                }
            }

            JsonObject result = new JsonObject();
            result.add("items", items);
            result.addProperty("total", subtotal);
            result.addProperty("item_count", itemCount);
            res.getWriter().print(result.toString());
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    // POST /api/cart
    //   action=add (default): item_id, quantity (default 1), adds to the cart
    //   action=set: item_id, quantity, sets the exact quantity; 0 removes the item
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
        if (action == null) action = "add";

        int itemId;
        int quantity;
        try {
            itemId = Integer.parseInt(req.getParameter("item_id"));
            String q = req.getParameter("quantity");
            quantity = (q == null) ? 1 : Integer.parseInt(q);
        } catch (NumberFormatException e) {
            sendError(res, 400, "item_id and quantity must be numbers");
            return;
        }

        if (!action.equals("add") && !action.equals("set")) {
            sendError(res, 400, "unknown action: " + action);
            return;
        }
        if (action.equals("add") && quantity < 1) {
            sendError(res, 400, "quantity must be at least 1");
            return;
        }
        if (action.equals("set") && quantity < 0) {
            sendError(res, 400, "quantity cannot be negative");
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            int cartId = findCartId(conn,customerId);
            if (cartId < 0) {
                sendError(res, 404, "No cart found for this customer");
                return;
            }

            if (action.equals("add")) {
                String sql = "INSERT INTO CART_ITEM (cart_id, item_id, quantity) VALUES (?, ?, ?) " +
                             "ON CONFLICT (cart_id, item_id) " +
                             "DO UPDATE SET quantity = CART_ITEM.quantity + EXCLUDED.quantity";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, cartId);
                    ps.setInt(2, itemId);
                    ps.setInt(3, quantity);
                    ps.executeUpdate();
                }
            } else if (quantity == 0) {
                String sql = "DELETE FROM CART_ITEM WHERE cart_id = ? AND item_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, cartId);
                    ps.setInt(2, itemId);
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE CART_ITEM SET quantity = ? WHERE cart_id = ? AND item_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, cartId);
                    ps.setInt(3, itemId);
                    ps.executeUpdate();
                }
            }

            res.getWriter().print("{\"status\":\"ok\"}");
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    private int findCartId(Connection conn,int customerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT cart_id FROM CART WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cart_id") : -1;
            }
        }
    }

    private void sendError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        res.getWriter().print(error.toString());
    }
}