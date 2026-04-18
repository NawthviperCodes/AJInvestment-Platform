package com.aj.investment.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(name = "RegisterClientServlet", urlPatterns = {"/RegisterClient"})
public class RegisterClientServlet extends HttpServlet {

    // TODO: Restore full implementation after model classes are generated

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"not ready\",\"message\":\"Setup in progress\"}");
    }
}