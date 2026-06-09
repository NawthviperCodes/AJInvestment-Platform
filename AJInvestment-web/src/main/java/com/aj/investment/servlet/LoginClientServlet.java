package com.aj.investment.servlet;

import com.aj.investment.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "LoginClientServlet", urlPatterns = {"/LoginClient"})
public class LoginClientServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginClientServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String username = trimOrEmpty(request.getParameter("username"));
        String password = trimOrEmpty(request.getParameter("password"));

        if (username.isEmpty() || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(json("error", "Username and password are required"));
            out.flush();
            return;
        }

        String hashedPassword = sha256(password);
        if (hashedPassword == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(json("error", "Could not process login. Please try again."));
            out.flush();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            ClientRecord client = findClient(conn, username, hashedPassword);

            if (client == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(json("error", "Invalid username/email or password"));
                out.flush();
                return;
            }

            String token = UUID.randomUUID().toString();
            response.setStatus(HttpServletResponse.SC_OK);
            out.print("{\"status\":\"success\","
                + "\"message\":\"Login successful\","
                + "\"token\":\"" + token + "\","
                + "\"userName\":\"" + escapeJson(client.displayName()) + "\"}");
            out.flush();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Client login failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(json("error", "Login failed. Please try again."));
            out.flush();
        }
    }

    private ClientRecord findClient(Connection conn, String usernameOrEmail, String hashedPassword)
            throws Exception {

        String sql = """
            SELECT id, firstName, lastName, username, email
            FROM Logdata
            WHERE (username = ? OR email = ?)
              AND password = ?
            LIMIT 1
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            ps.setString(3, hashedPassword);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new ClientRecord(
                    rs.getInt("id"),
                    safe(rs.getString("firstName")),
                    safe(rs.getString("lastName")),
                    safe(rs.getString("username")),
                    safe(rs.getString("email"))
                );
            }
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SHA-256 hashing failed", e);
            return null;
        }
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String json(String status, String message) {
        return "{\"status\":\"" + status + "\",\"message\":\""
            + escapeJson(message) + "\"}";
    }

    private String escapeJson(String value) {
        return value == null ? "" : value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private record ClientRecord(int id, String firstName, String lastName,
                                String username, String email) {
        String displayName() {
            String fullName = (firstName + " " + lastName).trim();
            if (!fullName.isBlank()) {
                return fullName;
            }
            if (!username.isBlank()) {
                return username;
            }
            return email;
        }
    }
}
