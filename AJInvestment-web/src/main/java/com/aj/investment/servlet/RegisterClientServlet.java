package com.aj.investment.servlet;

import com.aj.investment.db.DBConnection;
import com.aj.investment.service.EmailService;      // ← NEW: EJB injection
import com.aj.investment.service.FileUploadService;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@WebServlet(name = "RegisterClientServlet", urlPatterns = {"/RegisterClient"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize       = 5 * 1024 * 1024,
    maxRequestSize    = 10 * 1024 * 1024
)
public class RegisterClientServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RegisterClientServlet.class.getName());

    private static final String UPLOAD_DIR_PROP = "aj.upload.dir";
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[a-zA-Z0-9_]{4,30}$");
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^\\d{7,15}$");

    /**
     * ═════════════════════════════════════════════════════════════════════
     * EJB INJECTIONS (Container-Managed)
     * 
     * These EJBs are automatically managed by WildFly:
     *   • FileUploadService: Handles file storage
     *   • EmailService: Handles email sending
     * ══════════════════════════════════��══════════════════════════════════
     */
    @EJB
    private FileUploadService fileUploadService;

    @EJB
    private EmailService emailService;  // ← NEW: Email EJB

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            LOGGER.info("════════════════════════════════════════════════════════════");
            LOGGER.info("STEP A: SERVLET - Received registration request");
            LOGGER.info("════════════════════════════════════════════════════════════");

            // ─────────────────────────────────────────────────────────────────────
            // Extract text fields
            // ─────────────────────────────────────────────────────────────────────
            String firstName   = trimOrEmpty(request.getParameter("firstName"));
            String lastName    = trimOrEmpty(request.getParameter("lastName"));
            String email       = trimOrEmpty(request.getParameter("email"));
            String countryCode = trimOrEmpty(request.getParameter("countryCode"));
            String telephone   = trimOrEmpty(request.getParameter("telephone"));
            String username    = trimOrEmpty(request.getParameter("username"));
            String password    = trimOrEmpty(request.getParameter("password"));
            String confirm     = trimOrEmpty(request.getParameter("confirmPassword"));

            LOGGER.info("Text fields extracted: username=" + username + ", email=" + email);

            // ─────────────────────────────────────────────────────────────────────
            // Validate text fields
            // ─────────────────────────────────────────────────────────────────────
            String validationError = validate(
                firstName, lastName, email, countryCode, telephone, username, password, confirm);

            if (validationError != null) {
                LOGGER.warning("Validation failed: " + validationError);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(json("error", validationError));
                out.flush();
                return;
            }

            LOGGER.info("✓ All text fields validated successfully");

            // ─────────────────────────────────────────────────────────────────────
            // STEP B: Call FileUploadService EJB
            // ─────────────────────────────────────────────────────────────────────
            String savedFileName = null;
            Part picturePart = request.getPart("profilePicture");

            if (picturePart != null && picturePart.getSize() > 0) {
                LOGGER.info("Profile picture found: " + picturePart.getSubmittedFileName() 
                    + " (" + picturePart.getSize() + " bytes)");

                try {
                    String uploadDir = getUploadDir();
                    LOGGER.info("STEP B: Calling EJB (FileUploadService.saveProfilePicture)");
                    LOGGER.info("  → This automatically starts a CONTAINER-MANAGED TRANSACTION");
                    
                    savedFileName = fileUploadService.saveProfilePicture(picturePart, uploadDir);
                    
                    LOGGER.info("STEP B: EJB completed. File saved and callback registered.");
                    LOGGER.info("  → File path: " + savedFileName);

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Profile picture upload failed", e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print(json("error", "Failed to save profile picture: " + e.getMessage()));
                    out.flush();
                    return;
                }
            }

            // ─────────────────────────────────────────────────────────────────────
            // Hash password
            // ─────────────────────────────────────────────────────────────────────
            String hashedPassword = sha256(password);
            if (hashedPassword == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(json("error", "Password hashing failed"));
                out.flush();
                return;
            }

            // ─────────────────────────────────────────────────────────────────────
            // STEP C: Database persistence
            // ─────────────────────────────────────────────────────────────────────
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    LOGGER.info("STEP C: Database operations");
                    LOGGER.info("  → Checking email uniqueness...");
                    
                    if (exists(conn, "SELECT 1 FROM Logdata WHERE email = ?", email)) {
                        LOGGER.warning("Email already registered: " + email);
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.print(json("error", "Email address is already registered"));
                        conn.rollback();
                        LOGGER.info("✗ Transaction ROLLED BACK (email conflict)");
                        LOGGER.info("  → Cleanup callback TRIGGERED by Registry");
                        LOGGER.info("  → File DELETED from filesystem");
                        out.flush();
                        return;
                    }

                    LOGGER.info("  → Checking username uniqueness...");
                    if (exists(conn, "SELECT 1 FROM Logdata WHERE username = ?", username)) {
                        LOGGER.warning("Username already taken: " + username);
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.print(json("error", "Username is already taken"));
                        conn.rollback();
                        LOGGER.info("✗ Transaction ROLLED BACK (username conflict)");
                        LOGGER.info("  → Cleanup callback TRIGGERED by Registry");
                        LOGGER.info("  → File DELETED from filesystem");
                        out.flush();
                        return;
                    }

                    LOGGER.info("  → Getting Active status ID...");
                    int statusId = getActiveStatusId(conn);

                    LOGGER.info("  → Inserting record into Logdata table...");
                    int logdataId = insertLogdata(conn,
                        firstName, lastName, email,
                        countryCode, telephone,
                        username, hashedPassword,
                        statusId, savedFileName);

                    LOGGER.info("  → Logdata inserted with ID: " + logdataId);

                    LOGGER.info("  → Inserting record into Clients table...");
                    insertClient(conn, logdataId);

                    LOGGER.info("  → Generating email verification token...");
                    String token = UUID.randomUUID().toString();
                    insertEmailVerificationToken(conn, logdataId, token);

                    LOGGER.info("STEP D: Committing database transaction...");
                    conn.commit();
                    LOGGER.info("✓ Database transaction COMMITTED SUCCESSFULLY");

                    // ─────────────────────────────────────────────────────────────
                    // STEP E: Call EmailService EJB to send verification email
                    // 
                    // If this fails, it's non-fatal (registration already committed)
                    // ─────────────────────────────────────────────────────────────
                    try {
                        String verifyUrl = buildVerifyUrl(request, token);
                        LOGGER.info("STEP E: Calling EJB (EmailService.sendVerificationEmail)");
                        LOGGER.info("  → Verify URL: " + verifyUrl);
                        
                        emailService.sendVerificationEmail(email, firstName, token, verifyUrl);
                        
                        LOGGER.info("✓ Verification email sent successfully");
                    } catch (Exception e) {
                        // Non-fatal — registration already committed
                        LOGGER.log(Level.WARNING, 
                            "Failed to send verification email (non-fatal)", e);
                        System.err.println("Email send failed (non-fatal): " + e.getMessage());
                    }

                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"status\":\"success\","
                        + "\"message\":\"Account created. Please verify your email.\"}");
                    out.flush();

                } catch (SQLException e) {
                    try {
                        LOGGER.log(Level.SEVERE, "Database error occurred: " + e.getMessage(), e);
                        conn.rollback();
                        LOGGER.info("✗ Transaction ROLLED BACK (database error)");
                        LOGGER.info("  → Cleanup callback TRIGGERED by Registry");
                        LOGGER.info("  → File DELETED from filesystem");
                    } catch (SQLException rollbackEx) {
                        LOGGER.log(Level.SEVERE, "Rollback failed", rollbackEx);
                    }
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print(json("error", "Database error. Please try again."));
                    out.flush();
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database connection error", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(json("error", "Could not connect to database"));
                out.flush();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in RegisterClientServlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(json("error", "An unexpected error occurred"));
            out.flush();
        }
    }

    // ── Validation ────────────────────────────────────────────────────────
    private String validate(String firstName, String lastName,
                            String email, String countryCode,
                            String telephone, String username,
                            String password, String confirm) {

        if (firstName.length() < 2) 
            return "First name must be at least 2 characters";
        if (lastName.length() < 2) 
            return "Last name must be at least 2 characters";
        if (!EMAIL_PATTERN.matcher(email).matches())
            return "Invalid email address";
        if (countryCode.isEmpty()) 
            return "Country code is required";

        String rawPhone = telephone.replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(rawPhone).matches()) 
            return "Invalid telephone number (7-15 digits)";

        if (!USERNAME_PATTERN.matcher(username).matches())
            return "Username must be 4-30 characters (letters, numbers, underscores)";
        if (password.length() < 8) 
            return "Password must be at least 8 characters";
        if (!password.equals(confirm)) 
            return "Passwords do not match";

        return null;
    }

    // ── DB helpers ────────────────────────────────────────────────────────
    private boolean exists(Connection conn, String sql, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int getActiveStatusId(Connection conn) throws SQLException {
        String sql = "SELECT id FROM LogdataStatus WHERE status = 'Active' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
            throw new SQLException("Active status not found");
        }
    }

    private int insertLogdata(Connection conn,
                              String firstName, String lastName, String email,
                              String countryCode, String telephone,
                              String username, String hashedPassword,
                              int statusId, String profilePicture) throws SQLException {

        String sql = """
            INSERT INTO Logdata
                (firstName, lastName, email, countryCode, telephone,
                 username, password, status_id, profilePicture)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, countryCode);
            ps.setString(5, telephone);
            ps.setString(6, username);
            ps.setString(7, hashedPassword);
            ps.setInt(8, statusId);
            ps.setString(9, profilePicture);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No generated key for Logdata insert");
            }
        }
    }

    private void insertClient(Connection conn, int logdataId) throws SQLException {
        String sql = "INSERT INTO Clients (logDataId) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, logdataId);
            ps.executeUpdate();
        }
    }

    private void insertEmailVerificationToken(Connection conn,
                                               int logdataId,
                                               String token) throws SQLException {
        String sql = """
            INSERT INTO LogdataID_Email_verification_token
                (logdata_id, email_verification_token)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE
                email_verification_token = VALUES(email_verification_token),
                created_at = CURRENT_TIMESTAMP
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, logdataId);
            ps.setString(2, token);
            ps.executeUpdate();
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────
    private String getUploadDir() {
        String uploadBase = System.getProperty(UPLOAD_DIR_PROP);
        if (uploadBase == null || uploadBase.isBlank()) {
            uploadBase = System.getProperty("jboss.server.base.dir")
                + java.io.File.separator + "aj-investment-uploads" + java.io.File.separator;
            LOGGER.warning("Using fallback upload dir: " + uploadBase);
        }
        return uploadBase;
    }

    private String buildVerifyUrl(HttpServletRequest req, String token) {
        String scheme      = req.getScheme();
        String host        = req.getServerName();
        int    port        = req.getServerPort();
        String contextPath = req.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(host);

        boolean defaultPort = ("http".equals(scheme)  && port == 80)
                           || ("https".equals(scheme) && port == 443);
        if (!defaultPort) url.append(':').append(port);

        url.append(contextPath).append("/VerifyEmail?token=").append(token);
        return url.toString();
    }

    private String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
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
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SHA-256 hashing failed", e);
            return null;
        }
    }
}