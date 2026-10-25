package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.gson.JsonObject;

@WebServlet("/api/checkout")
public class CheckoutServlet extends HttpServlet {
    static final String URL = "jdbc:postgresql://localhost:5432/food_delivery";
    static final String USER = "postgres";
    static final String PASS = "YOUR_PASSWORD";
        static final double DELIVERY_FEE = 2.99;



    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static class CartRow {
        final int itemId;
        final int restaurantId;
        final double price;
        final int qty;

        CartRow(int itemId, int restaurantId, double price, int qty) {
            this.itemId = itemId;
            this.restaurantId = restaurantId;
            this.price = price;
            this.qty = qty;
        }
    }

    // POST /api/checkout  (optional form field: method = UPI | Card | Cash on Delivery)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        Integer customerId = Auth.currentCustomer(req);
        if (customerId == null) {
            sendError(res, 401, "Please log in");
            return;
        }

        String method = req.getParameter("method");
        if (method == null) method = "Cash on Delivery";
        if (!method.equals("UPI") && !method.equals("Card") && !method.equals("Cash on Delivery")) {
            sendError(res, 400, "unknown payment method: " + method);
            return;
        }
        String addressParam = req.getParameter("address_id");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);
            try {
                int cartId = findCartId(conn,customerId);
                if (cartId < 0) {
                    sendError(res, 404, "No cart found for this customer");
                    return;
                }

                // Read the cart with current menu prices
                List<CartRow> rows = new ArrayList<>();
                String cartSql = "SELECT m.item_id, m.restaurant_id, m.price, ci.quantity " +
                                 "FROM CART_ITEM ci JOIN MENU_ITEM m ON m.item_id = ci.item_id " +
                                 "WHERE ci.cart_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(cartSql)) {
                    ps.setInt(1, cartId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            rows.add(new CartRow(
                                rs.getInt("item_id"),
                                rs.getInt("restaurant_id"),
                                rs.getDouble("price"),
                                rs.getInt("quantity")));
                        }
                    }
                }

                if (rows.isEmpty()) {
                    sendError(res, 400, "Your cart is empty");
                    return;
                }

                // Business rule: every item in an order must come from the same restaurant
                Set<Integer> restaurants = new HashSet<>();
                double total = 0;
                for (CartRow r : rows) {
                    restaurants.add(r.restaurantId);
                    total += r.price * r.qty;
                }
                if (restaurants.size() != 1) {
                    sendError(res, 400, "All items in an order must come from the same restaurant");
                    return;
                }
                int restaurantId = restaurants.iterator().next();

                // 1. Create the order
                                // Total charged = items + delivery fee
                double grandTotal = total + DELIVERY_FEE;

                // 1. Create the order (total_amount includes the delivery fee)
                int orderId;
                String orderSql = "INSERT INTO ORDERS (customer_id, restaurant_id, total_amount, status, delivery_address_id) " +
                                  "VALUES (?, ?, ?, 'Placed', ?) RETURNING order_id";
                try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, restaurantId);
                    ps.setDouble(3, grandTotal);
                    if (addressParam == null || addressParam.isBlank()) ps.setNull(4, Types.INTEGER);
                    else ps.setInt(4, Integer.parseInt(addressParam));
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        orderId = rs.getInt(1);
                    }
                }

                // 2. Copy the cart rows into order items, saving the price at the time of ordering
                String itemSql = "INSERT INTO ORDER_ITEM (order_id, item_id, quantity, unit_price) " +
                                 "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (CartRow r : rows) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, r.itemId);
                        ps.setInt(3, r.qty);
                        ps.setDouble(4, r.price);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 3. Create the payment, pending until it's confirmed
                                // 3. Create the payment, marked as completed
                String paySql = "INSERT INTO PAYMENT (order_id, amount, method, status) " +
                                "VALUES (?, ?, ?, 'Completed')";
                try (PreparedStatement ps = conn.prepareStatement(paySql)) {
                    ps.setInt(1, orderId);
                    ps.setDouble(2, grandTotal);
                    ps.setString(3, method);
                    ps.executeUpdate();
                }

                // 4. Empty the cart
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CART_ITEM WHERE cart_id = ?")) {
                    ps.setInt(1, cartId);
                    ps.executeUpdate();
                }
                Dispatcher.run(conn);
                conn.commit();

                JsonObject result = new JsonObject();
                result.addProperty("order_id", orderId);
                result.addProperty("total", grandTotal);
                result.addProperty("status", "Placed");
                res.getWriter().print(result.toString());
            } catch (SQLException e) {
                conn.rollback();
                sendError(res, 500, e.getMessage());
            }
        } catch (SQLException e) {
            sendError(res, 500, e.getMessage());
        }
    }

    private int findCartId(Connection conn,int customerId ) throws SQLException {
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