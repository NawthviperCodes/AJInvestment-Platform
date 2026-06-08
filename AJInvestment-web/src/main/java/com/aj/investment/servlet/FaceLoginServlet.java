package com.aj.investment.servlet;

import com.aj.investment.db.DBConnection;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "FaceLoginServlet", urlPatterns = {"/FaceLogin"})
public class FaceLoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FaceLoginServlet.class.getName());
    private static final float MIN_FACE_CONFIDENCE = 0.70f;
    private final Gson gson = new Gson();

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        applyJsonHeaders(response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        applyJsonHeaders(response);
        PrintWriter out = response.getWriter();

        try {
            LoginRequest loginRequest = gson.fromJson(request.getReader(), LoginRequest.class);
            String validationError = validate(loginRequest);

            if (validationError != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(LoginResponse.error(validationError)));
                out.flush();
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                ClientRecord client = findClient(conn, loginRequest.userId.trim());

                if (client == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print(gson.toJson(LoginResponse.error("Client was not found in AJ Investment")));
                    out.flush();
                    return;
                }

                if (loginRequest.faceConfidence < MIN_FACE_CONFIDENCE) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print(gson.toJson(LoginResponse.error("Face confidence is too low")));
                    out.flush();
                    return;
                }

                String token = UUID.randomUUID().toString();
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(gson.toJson(LoginResponse.success(token, client.displayName())));
                out.flush();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Face login failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(LoginResponse.error("Face login failed. Please try again.")));
            out.flush();
        }
    }

    private void applyJsonHeaders(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
    }

    private String validate(LoginRequest request) {
        if (request == null) {
            return "Missing login request";
        }
        if (request.userId == null || request.userId.trim().isEmpty()) {
            return "User ID is required";
        }
        if (request.deviceId == null || request.deviceId.trim().isEmpty()) {
            return "Device ID is required";
        }
        if (request.faceConfidence < 0.0f || request.faceConfidence > 1.0f) {
            return "Face confidence must be between 0 and 1";
        }
        return null;
    }

    private ClientRecord findClient(Connection conn, String userId) throws SQLException {
        String sql = """
            SELECT id, firstName, lastName, username, email
            FROM Logdata
            WHERE username = ?
               OR email = ?
               OR CAST(id AS CHAR) = ?
            LIMIT 1
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setString(3, userId);

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

    private String safe(String value) {
        return value == null ? "" : value.trim();
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

    private static class LoginRequest {
        String userId;
        float faceConfidence;
        String deviceId;
    }

    private static class LoginResponse {
        boolean success;
        String token;
        String userName;
        String message;

        static LoginResponse success(String token, String userName) {
            LoginResponse response = new LoginResponse();
            response.success = true;
            response.token = token;
            response.userName = userName;
            response.message = "Face login successful";
            return response;
        }

        static LoginResponse error(String message) {
            LoginResponse response = new LoginResponse();
            response.success = false;
            response.message = message;
            return response;
        }
    }
}
