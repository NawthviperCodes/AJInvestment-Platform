/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.aj.investment.servlet;

 
import com.aj.investment.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

@WebServlet("/test")
public class TestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<html><body>");
        out.println("<h2>AJ Investment Backend</h2>");

        // Test database connection
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                out.println("<p style='color:green;'>Database connection successful!</p>");
            }
        } catch (Exception e) {
            out.println("<p style='color:red;'>Database connection failed: " + e.getMessage() + "</p>");
        }

        out.println("</body></html>");
    }
}