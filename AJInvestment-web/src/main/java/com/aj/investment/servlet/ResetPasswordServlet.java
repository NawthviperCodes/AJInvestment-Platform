package com.aj.investment.servlet;

import com.aj.investment.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "ResetPasswordServlet", urlPatterns = {"/ResetPassword"})
public class ResetPasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ResetPasswordServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String token = trimOrEmpty(request.getParameter("token"));
        String password = trimOrEmpty(request.getParameter("password"));
        String confirmPassword = trimOrEmpty(request.getParameter("confirmPassword"));

        String validationError = validate(token, password, confirmPassword);
        if (validationError != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(json("error", validationError));
            out.flush();
            return;
        }

        String hashedPassword = sha256(password);
        if (hashedPassword == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(json("error", "Password hashing failed"));
            out.flush();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                Integer logdataId = findLogdataIdByToken(conn, token);
                if (logdataId == null) {
                    conn.rollback();
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(json("error", "Reset link is invalid or expired"));
                    out.flush();
                    return;
                }

                updatePassword(conn, logdataId, hashedPassword);
                deleteResetTokens(conn, logdataId);
                conn.commit();

                response.setStatus(HttpServletResponse.SC_OK);
                out.print(json("success", "Password updated. You can sign in now."));
                out.flush();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Password reset failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(json("error", "Could not reset password. Please try again."));
            out.flush();
        }
    }

    private Integer findLogdataIdByToken(Connection conn, String token) throws SQLException {
        String sql = """
            SELECT logdata_id
            FROM PasswordResetTokens
            WHERE token = ?
              AND expires_at > NOW()
            LIMIT 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("logdata_id") : null;
            }
        }
    }

    private void updatePassword(Connection conn, int logdataId, String hashedPassword) throws SQLException {
        String sql = "UPDATE Logdata SET password = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, logdataId);
            ps.executeUpdate();
        }
    }

    private void deleteResetTokens(Connection conn, int logdataId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM PasswordResetTokens WHERE logdata_id = ?")) {
            ps.setInt(1, logdataId);
            ps.executeUpdate();
        }
    }

    private String validate(String token, String password, String confirmPassword) {
        if (token.isEmpty()) {
            return "Reset link is missing a token";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match";
        }
        return null;
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String json(String status, String message) {
        String escaped = message.replace("\\", "\\\\")
                               .replace("\"", "\\\"")
                               .replace("\n", "\\n")
                               .replace("\r", "\\r");
        return "{\"status\":\"" + status + "\",\"message\":\"" + escaped + "\"}";
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
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
}
