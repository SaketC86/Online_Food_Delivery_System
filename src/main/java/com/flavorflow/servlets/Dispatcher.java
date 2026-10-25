package com.flavorflow.servlets;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dispatcher {

    // Moves orders along by age, frees drivers who finished, and assigns
    // free drivers to orders that don't have one yet.
    // Uses the caller's connection and does NOT commit; the caller decides.
    public static void run(Connection conn) throws SQLException {

        // 1. Advance order statuses by age (1, 3, and 6 minutes)
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                "UPDATE ORDERS SET status = 'Delivered' " +
                "WHERE status IN ('Placed','Preparing','Out-for-delivery') " +
                "AND now() - order_date >= interval '2 minutes'");
            st.executeUpdate(
                "UPDATE ORDERS SET status = 'Out-for-delivery' " +
                "WHERE status IN ('Placed','Preparing') " +
                "AND now() - order_date >= interval '1 minute'");
            st.executeUpdate(
                "UPDATE ORDERS SET status = 'Preparing' " +
                "WHERE status = 'Placed' " +
                "AND now() - order_date >= interval '20 seconds'");
        }

        // 2. Mark each driver Busy if they have an active order, otherwise Free
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                "UPDATE DELIVERY_AGENT a SET availability_status = CASE " +
                "  WHEN EXISTS (SELECT 1 FROM ORDERS o WHERE o.agent_id = a.agent_id " +
                "               AND o.status IN ('Placed','Preparing','Out-for-delivery')) " +
                "  THEN 'Busy' ELSE 'Free' END");
        }

        // 3. Give free drivers to active orders that don't have one, oldest first
        List<Integer> unassigned = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT order_id FROM ORDERS " +
                "WHERE agent_id IS NULL " +
                "AND status IN ('Placed','Preparing','Out-for-delivery') " +
                "ORDER BY order_date, order_id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    unassigned.add(rs.getInt(1));
                }
            }
        }

        for (int orderId : unassigned) {
            Integer agentId = findFreeAgent(conn);
            if (agentId == null) {
                break; // no free drivers; the remaining orders wait
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ORDERS SET agent_id = ? WHERE order_id = ?")) {
                ps.setInt(1, agentId);
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE DELIVERY_AGENT SET availability_status = 'Busy' WHERE agent_id = ?")) {
                ps.setInt(1, agentId);
                ps.executeUpdate();
            }
        }
    }

    // Picks one free driver and locks the row, so two requests can't grab the same driver
    private static Integer findFreeAgent(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT agent_id FROM DELIVERY_AGENT " +
                "WHERE availability_status = 'Free' " +
                "ORDER BY agent_id LIMIT 1 FOR UPDATE SKIP LOCKED")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }
}