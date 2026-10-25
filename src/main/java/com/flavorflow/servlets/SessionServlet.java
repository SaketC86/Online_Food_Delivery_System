package com.flavorflow.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/session")
public class SessionServlet extends HttpServlet {

    // GET /api/session: 200 if logged in, 401 if not. Used by pages to redirect.
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        if (Auth.currentCustomer(req) == null) {
            res.setStatus(401);
            res.getWriter().print("{\"error\":\"Please log in\"}");
        } else {
            res.getWriter().print("{\"status\":\"ok\"}");
        }
    }
}