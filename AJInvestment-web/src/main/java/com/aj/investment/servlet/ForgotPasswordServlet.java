package com.aj.investment.servlet;

import com.aj.investment.db.DBConnection;
import com.aj.investment.service.EmailService;
import jakarta.ejb.EJB;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {"/ForgotPassword"})
public class ForgotPasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    @EJB
    private EmailService emailService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String email = trimOrEmpty(request.getParameter("email")).toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(json("error", "Enter a valid email address"));
            out.flush();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            UserRecord user = findUserByEmail(conn, email);

            if (user == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(json("success", "If that email exists, a reset link has been sent."));
                out.flush();
                return;
            }

            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
            saveResetToken(conn, user.logdataId(), token, expiresAt);

            String resetUrl = buildResetUrl(request, token);
            emailService.sendPasswordResetEmail(user.email(), user.firstName(), token, resetUrl);

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(json("success", "Password reset link sent. Please check your email."));
            out.flush();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Forgot password request failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(json("error", "Could not send reset link. Please try again."));
            out.flush();
        }
    }

    private UserRecord findUserByEmail(Connection conn, String email) throws SQLException {
        String sql = """
            SELECT id, firstName, email
            FROM Logdata
            WHERE LOWER(email) = ?
            LIMIT 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UserRecord(
                    rs.getInt("id"),
                    trimOrEmpty(rs.getString("firstName")),
                    rs.getString("email")
                );
            }
        }
    }

    private void saveResetToken(Connection conn, int logdataId, String token,
                                LocalDateTime expiresAt) throws SQLException {
        try (PreparedStatement delete = conn.prepareStatement(
                "DELETE FROM PasswordResetTokens WHERE logdata_id = ?")) {
            delete.setInt(1, logdataId);
            delete.executeUpdate();
        }

        String sql = """
            INSERT INTO PasswordResetTokens (logdata_id, token, expires_at)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, logdataId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    private String buildResetUrl(HttpServletRequest req, String token) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String contextPath = req.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(host);

        boolean defaultPort = ("http".equals(scheme) && port == 80)
                           || ("https".equals(scheme) && port == 443);
        if (!defaultPort) {
            url.append(':').append(port);
        }

        url.append(contextPath).append("/reset_password.html?token=").append(token);
        return url.toString();
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

    private record UserRecord(int logdataId, String firstName, String email) {}
}
