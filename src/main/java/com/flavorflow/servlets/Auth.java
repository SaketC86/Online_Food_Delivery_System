package com.flavorflow.servlets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class Auth {
    public static final String SESSION_KEY = "customerId";

    // Returns the logged-in customer's ID, or null if nobody is logged in
    public static Integer currentCustomer(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute(SESSION_KEY);
        return (value instanceof Integer) ? (Integer) value : null;
    }
}