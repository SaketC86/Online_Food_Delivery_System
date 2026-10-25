package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/api/restaurants")
public class RestaurantServlet extends HttpServlet {
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
        
        JsonArray restaurants = new JsonArray();
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM RESTAURANT ORDER BY rating DESC")) {
            
            while (rs.next()) {
                JsonObject rest = new JsonObject();
                rest.addProperty("id", rs.getInt("restaurant_id"));
                rest.addProperty("name", rs.getString("name"));
                rest.addProperty("location", rs.getString("location"));
                rest.addProperty("cuisine_type", rs.getString("cuisine_type"));
                rest.addProperty("rating", rs.getDouble("rating"));
                rest.addProperty("min_order", 15.00); // Default, adjust as needed
                rest.addProperty("delivery_time", "20-30 min"); // Default
                
                restaurants.add(rest);
            }
            res.getWriter().print(restaurants.toString());
        } catch (SQLException e) {
            res.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            res.getWriter().print(error.toString());
        }
    }
}
