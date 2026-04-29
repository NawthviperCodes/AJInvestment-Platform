package com.aj.investment.service;

import com.aj.investment.transaction.FileUploadCleanupCallback;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.Part;
 import jakarta.transaction.TransactionSynchronizationRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * STEP B: THE EJB (The Worker)
 * 
 * This is a @Stateless EJB, which means:
 *   1. WildFly creates a CONTAINER-MANAGED TRANSACTION automatically
 *   2. When the Servlet calls this EJB, the TX starts
 *   3. When this EJB finishes, the TX is ready to COMMIT
 * 
 * Our job:
 *   • Save the file to disk
 *   • Register a cleanup callback with the TransactionSynchronizationRegistry
 *   • If the TX fails later (in the Servlet), our callback gets called
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Stateless
public class FileUploadService {

    private static final Logger LOGGER = Logger.getLogger(FileUploadService.class.getName());
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp"};

    /**
     * ═════════════════════════════════════════════════════════════════════
     * CRITICAL: This @Resource injection brings in the Registry
     * 
     * The TransactionSynchronizationRegistry is a WildFly Container service
     * that manages transaction callbacks. It's the BRAIN of our cleanup system.
     * 
     * At this exact moment (when the EJB is instantiated):
     *   • WildFly has ALREADY started a TRANSACTION
     *   • This Registry is BOUND to that transaction
     *   • We can now use it to register cleanup logic
     * ═════════════════════════════════════════════════════════════════════
     */
    @Inject
    private TransactionSynchronizationRegistry txRegistry;

    /**
     * Save a profile picture and register cleanup callback.
     *
     * EXECUTION TIMELINE:
     *   1. Servlet calls this method
     *   2. WildFly starts a CONTAINER-MANAGED TRANSACTION
     *   3. This method runs INSIDE that transaction
     *   4. We save the file to disk
     *   5. We "tag" the cleanup logic into the Registry
     *   6. This method returns
     *   7. Control goes back to the Servlet
     *   8. Servlet does DB work
     *   9. Servlet commits the transaction
     *   10. Registry decides: Execute cleanup? Or discard?
     *
     * @param part the multipart file from the request
     * @param uploadDir the base upload directory (e.g., C:\...\aj-investment-uploads\)
     * @return relative file path (e.g., "profile-pictures/uuid.jpg")
     * @throws IOException if file I/O fails
      */
    /**
 * Save a profile picture and register cleanup callback.
 *
 * @param part the multipart file from the request
 * @param uploadDir the base upload directory
 * @return relative file path (e.g., "profile-pictures/uuid.jpg")
 * @throws IOException if file I/O fails or registration fails
 */
public String saveProfilePicture(Part part, String uploadDir) 
        throws IOException {  // ← Removed SystemException

    LOGGER.info("════════════════════════════════════════════════════════════");
    LOGGER.info("STEP B: EJB - FileUploadService.saveProfilePicture() called");
    LOGGER.info("════════════════════════════════════════════════════════════");
    LOGGER.info("  → WildFly has ALREADY started a transaction");
    LOGGER.info("  → We are INSIDE that transaction now");

    // ─────────────────────────────────────────────────────────────────────
    // STEP B1: Validate the file
    // ────────���────────────────────────────────────────────────────────────
    if (part == null || part.getSize() == 0) {
        LOGGER.info("No file provided");
        return null;
    }

    String contentType = part.getContentType();
    LOGGER.info("STEP B1: File validation");
    LOGGER.info("  → Content-Type: " + contentType);
    LOGGER.info("  → File size: " + part.getSize() + " bytes");

    if (!isAllowedFileType(contentType)) {
        throw new IOException("Invalid file type: " + contentType + 
            ". Allowed: JPEG, PNG, WEBP");
    }

    if (part.getSize() > MAX_FILE_SIZE) {
        throw new IOException("File too large: " + part.getSize() + 
            " bytes (max " + MAX_FILE_SIZE + ")");
    }

    LOGGER.info("  ✓ File validation passed");

    // ─────────────────────────────────────────────────────────────────────
    // STEP B2: Prepare disk location
    // ─────────────────────────────────────────────────────────────────────
    String profilePicsDir = "profile-pictures";
    Path dir = Paths.get(uploadDir, profilePicsDir);
    
    LOGGER.info("STEP B2: Preparing disk location");
    LOGGER.info("  → Base dir: " + uploadDir);
    LOGGER.info("  → Full path: " + dir.toAbsolutePath());
    
    Files.createDirectories(dir);
    LOGGER.info("  ✓ Directory ensured to exist");

    // ─────────────────────────────────────────────────────────────────────
    // STEP B3: Generate unique filename
    // ─────────────────────────────────────────────────────────────────────
    String original = Paths.get(part.getSubmittedFileName())
        .getFileName().toString();
    String extension = getExtension(original);
    String uniqueName = UUID.randomUUID().toString() + extension;
    Path target = dir.resolve(uniqueName);

    LOGGER.info("STEP B3: Generating unique filename");
    LOGGER.info("  → Original: " + original);
    LOGGER.info("  → Extension: " + extension);
    LOGGER.info("  → UUID: " + uniqueName);
    LOGGER.info("  → Target: " + target.toAbsolutePath());

    // ─────────────────────────────────────────────────────────────────────
    // STEP B4: Write file to disk
    // ─────────────────────────────────────────────────────────────────────
    try (InputStream in = part.getInputStream()) {
        LOGGER.info("STEP B4: Writing file to disk...");
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("  ✓ File written successfully!");
        LOGGER.info("    Location: " + target.toAbsolutePath());
    } catch (IOException e) {
        // If write fails, clean up the partial file immediately
        try {
            Files.deleteIfExists(target);
        } catch (IOException deleteEx) {
            LOGGER.log(Level.WARNING, "Failed to clean up partial file: " + target, deleteEx);
        }
        throw e;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP B5: Register cleanup callback with Transaction Registry
    // ─────────────────────────────────────────────────────────────────────
    try {
        LOGGER.info("STEP B5: Registering cleanup callback");
        LOGGER.info("  → Creating FileUploadCleanupCallback");
        LOGGER.info("  → This callback will watch the transaction");
        
        FileUploadCleanupCallback callback = new FileUploadCleanupCallback(target);
        
        LOGGER.info("  → Registering callback with TransactionSynchronizationRegistry");
        txRegistry.registerInterposedSynchronization(callback);
        
        LOGGER.info("✓ Cleanup callback REGISTERED");
        LOGGER.info("  → If TX rolls back: File will be DELETED");
        LOGGER.info("  → If TX commits: File will be RETAINED");
        LOGGER.info("");
        LOGGER.info("  Callback is now WAITING for transaction to complete...");
        
    } catch (Exception e) {
        // If registration fails, clean up the file immediately
        LOGGER.log(Level.WARNING, 
            "Failed to register cleanup callback. Deleting file: " + target, e);
        try {
            Files.deleteIfExists(target);
        } catch (IOException deleteEx) {
            LOGGER.log(Level.WARNING, "Failed to delete file: " + target, deleteEx);
        }
        throw new IOException("Failed to register cleanup callback", e);  // ← Wrap in IOException
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP B6: Return relative path
    // ─────────────────────────────────────────────────────────────────────
    String relativePath = profilePicsDir + "/" + uniqueName;
    LOGGER.info("STEP B6: Returning to Servlet");
    LOGGER.info("  → Relative path: " + relativePath);
    LOGGER.info("");
    LOGGER.info("═══════════════════════════════════════════════════════════");
    LOGGER.info("EJB COMPLETED. Returning to Servlet for DB operations...");
    LOGGER.info("═══════════════════════════════════════════════════════════");
    
    return relativePath;
}
    /**
     * Validate if the file type is allowed.
     */
    private boolean isAllowedFileType(String contentType) {
        if (contentType == null) return false;
        for (String allowed : ALLOWED_TYPES) {
            if (contentType.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract file extension from filename.
     */
    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot).toLowerCase() : ".jpg";
    }
}