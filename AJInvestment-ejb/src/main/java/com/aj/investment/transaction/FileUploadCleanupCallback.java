package com.aj.investment.transaction;

import jakarta.transaction.Synchronization;
import jakarta.transaction.Status;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * STEP C: THE CLEANUP CALLBACK (The Judge)
 * 
 * This class implements the Synchronization interface, which means it has
 * a special method called afterCompletion() that gets called AFTER the
 * transaction completes (whether it commits or rolls back).
 * 
 * The Registry watches the transaction status:
 *   • If status = 3 (COMMITTED): We do nothing (file stays)
 *   • If status = 4 (ROLLED BACK): We delete the file
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class FileUploadCleanupCallback implements Synchronization {

    private static final Logger LOGGER = Logger.getLogger(FileUploadCleanupCallback.class.getName());
    private final Path filePath;

    public FileUploadCleanupCallback(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Called BEFORE the transaction completion.
     * At this point, resources are still available (file still on disk).
     * We don't do anything here — we wait for afterCompletion.
     */
    @Override
    public void beforeCompletion() {
        LOGGER.fine("beforeCompletion() called - Transaction about to complete");
    }

    /**
     * ═════════════════════════════════════════════════════════���═══════════
     * CRITICAL: Called AFTER transaction completion
     * 
     * This is where the MAGIC happens!
     * 
     * The status parameter tells us what happened:
     *   • STATUS_COMMITTED (3): Yay! Keep the file.
     *   • STATUS_ROLLEDBACK (4): Oh no! Delete the file.
     * ═════════════════════════════════════════════════════════════════════
     * 
     * EXECUTION SCENARIOS:
     * 
     * SCENARIO 1: Transaction Committed Successfully
     * ─────────────────────────────────────────────
     * Status = 3 (STATUS_COMMITTED)
     * Action: Log and return (file is kept)
     * 
     * ```
     * afterCompletion(3)
     *   ↓
     * status == STATUS_COMMITTED? YES
     *   ↓
     * Log: "Transaction committed. File retained"
     *   ↓
     * return (do nothing)
     * ```
     * 
     * SCENARIO 2: Transaction Rolled Back (Email Conflict)
     * ───────────��───────────────────────────────────────
     * Status = 4 (STATUS_ROLLEDBACK)
     * Action: Delete the file
     * 
     * ```
     * afterCompletion(4)
     *   ↓
     * status == STATUS_ROLLEDBACK? YES
     *   ↓
     * Files.delete(filePath)
     *   ↓
     * Log: "Cleaned up uploaded file on rollback"
     * ```
     * 
     * SCENARIO 3: Transaction Rolled Back (Database Error)
     * ───────────────────────────────────────────────────
     * Status = 4 (STATUS_ROLLEDBACK)
     * Action: Delete the file
     * 
     * ```
     * afterCompletion(4)
     *   ↓
     * status == STATUS_ROLLEDBACK? YES
     *   ↓
     * Files.delete(filePath)
     *   ↓
     * Log: "Cleaned up uploaded file on rollback"
     * ```
     */
    @Override
    public void afterCompletion(int status) {
        LOGGER.info("════════════════════════════════════════════════════════════");
        LOGGER.info("STEP C: CLEANUP CALLBACK - afterCompletion() called");
        LOGGER.info("════════════════════════════════════════════════════════════");
        LOGGER.info("Transaction Status: " + getStatusName(status));

        if (status == Status.STATUS_ROLLEDBACK) {
            // ────��────────────────────────────────────────────────────────
            // ROLLBACK OCCURRED
            // The Servlet's database operations failed.
            // We must now clean up the file.
            // ─────────────────────────────────────────────────────────────
            LOGGER.info("✗ STATUS_ROLLEDBACK (value=4)");
            LOGGER.info("  → The transaction FAILED");
            LOGGER.info("  → Reason: Database error or constraint violation");
            LOGGER.info("");
            LOGGER.info("CLEANUP ACTION:");
            LOGGER.info("  → Deleting file: " + filePath.toAbsolutePath());

            try {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    LOGGER.info("✓ File DELETED successfully");
                    LOGGER.info("");
                    LOGGER.info("════════════════════════════════════════════════════════════");
                    LOGGER.info("REGISTRATION COMPLETE (FAILURE)");
                    LOGGER.info("════════════════════════════════════════════════════════════");
                    LOGGER.info("Summary:");
                    LOGGER.info("  • File uploaded to disk ✓");
                    LOGGER.info("  • Database operation failed ✗");
                    LOGGER.info("  • Transaction rolled back ✗");
                    LOGGER.info("  • File cleaned up ✓");
                    LOGGER.info("");
                    LOGGER.info("Result: NO registration, NO file stored");
                } else {
                    LOGGER.info("  ⚠ File not found (already deleted?)");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, 
                    "Failed to delete file on rollback: " + filePath, e);
                // Non-fatal — log and continue
            }

        } else if (status == Status.STATUS_COMMITTED) {
            // ─────────────────────────────────────────────────────────────
            // COMMIT SUCCEEDED
            // Everything worked! Keep the file.
            // ─────────────────────────────────────────────────────────────
            LOGGER.info("✓ STATUS_COMMITTED (value=3)");
            LOGGER.info("  → The transaction SUCCEEDED");
            LOGGER.info("  → All database changes persisted");
            LOGGER.info("");
            LOGGER.info("CLEANUP ACTION:");
            LOGGER.info("  → NO cleanup needed");
            LOGGER.info("  → File is retained on filesystem");
            LOGGER.info("  → File path: " + filePath.toAbsolutePath());
            LOGGER.info("");
            LOGGER.info("════════════════════════════════════════════════════════════");
            LOGGER.info("REGISTRATION COMPLETE (SUCCESS)");
            LOGGER.info("════════════════════════════════════════════════════════════");
            LOGGER.info("Summary:");
            LOGGER.info("  • File uploaded to disk ✓");
            LOGGER.info("  • Database operation succeeded ✓");
            LOGGER.info("  • Transaction committed ✓");
            LOGGER.info("  • File retained ✓");
            LOGGER.info("");
            LOGGER.info("Result: Registration complete, file stored");

        } else {
            // Some other status (shouldn't happen in normal operation)
            LOGGER.info("⚠ Unexpected status: " + getStatusName(status));
        }
    }

    /**
     * Convert transaction status code to human-readable name
     */
    private String getStatusName(int status) {
        return switch (status) {
            case Status.STATUS_ACTIVE -> "ACTIVE (0)";
            case Status.STATUS_MARKED_ROLLBACK -> "MARKED_ROLLBACK (1)";
            case Status.STATUS_PREPARED -> "PREPARED (2)";
            case Status.STATUS_COMMITTED -> "COMMITTED (3)";
            case Status.STATUS_ROLLEDBACK -> "ROLLEDBACK (4)";
            case Status.STATUS_UNKNOWN -> "UNKNOWN (5)";
            case Status.STATUS_NO_TRANSACTION -> "NO_TRANSACTION (6)";
            case Status.STATUS_PREPARING -> "PREPARING (7)";
            case Status.STATUS_COMMITTING -> "COMMITTING (8)";
            case Status.STATUS_ROLLING_BACK -> "ROLLING_BACK (9)";
            default -> "UNKNOWN (" + status + ")";
        };
    }
}