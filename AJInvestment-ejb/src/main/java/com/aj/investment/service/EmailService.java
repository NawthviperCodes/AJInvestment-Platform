package com.aj.investment.service;

import jakarta.ejb.Stateless;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * EMAIL SERVICE (EJB Stateless)
 * 
 * Handles all email operations within a managed transaction context.
 * This belongs in the EJB module because:
 * 
 *   1. Container manages transaction lifecycle
 *   2. @Resource injection works natively
 *   3. Can be called from multiple servlets
 *   4. Supports @Asynchronous for non-blocking sends
 *   5. Better error handling with container rollback
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 */
@Stateless
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    // â”€â”€ SMTP Configuration â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
     private static final String SMTP_HOST = "mail.g.co.za";
    private static final int    SMTP_PORT = 587;
    private static final String SMTP_USER = "te@as.co.za";
    private static final String SMTP_PASS = "trte@pg";
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static final String FROM_ADDRESS = "noreply@ajinvestment.co.za";
    private static final String FROM_NAME    = "AJ Investment";

    /**
     * Send email verification link to newly registered client.
     *
     * Called from RegisterClientServlet AFTER the file is uploaded and
     * the transaction is about to commit.
     *
     * If this method throws an exception, the transaction rolls back and
     * the cleanup callback deletes the uploaded file.
     *
     * @param toEmail   recipient's email address
     * @param firstname recipient's first name
     * @param token     verification token (UUID)
     * @param verifyUrl complete verification URL (built by servlet)
     * @throws MessagingException if SMTP send fails
     */
    public void sendVerificationEmail(String toEmail,
                                      String firstname,
                                      String token,
                                      String verifyUrl)
            throws MessagingException {

        LOGGER.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        LOGGER.info("EMAIL SERVICE: Sending verification email");
        LOGGER.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        LOGGER.info("To:       " + toEmail);
        LOGGER.info("Name:     " + firstname);
        LOGGER.info("Token:    " + token);
        LOGGER.info("Verify URL: " + verifyUrl);

        Session mailSession = buildSession();
        MimeMessage msg = new MimeMessage(mailSession);

        try {
            msg.setFrom(new InternetAddress(FROM_ADDRESS, FROM_NAME, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            msg.setFrom(new InternetAddress(FROM_ADDRESS));
        }

        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        msg.setSubject("Verify your AJ Investment Account");
        msg.setContent(buildHtmlBody(firstname, verifyUrl), "text/html;charset=UTF-8");

        LOGGER.info("Sending email via SMTP (" + SMTP_HOST + ":" + SMTP_PORT + ")...");
        Transport.send(msg);
        LOGGER.info("âœ“ Email sent successfully!");
        LOGGER.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Build Jakarta Mail session with STARTTLS on port 587.
     * Mirrors PHPMailer: $mail->SMTPSecure = ENCRYPTION_STARTTLS; $mail->Port = 587;
     */
    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");   // ENCRYPTION_STARTTLS
        props.put("mail.smtp.starttls.required", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });
    }

    /**
     * Build professional HTML email body.
     */
    private String buildHtmlBody(String firstname, String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Verify your AJ Investment Account</title>
              <style>
                body { margin: 0; padding: 0; background: #f4f6f8; font-family: Arial, sans-serif; }
                table { border-collapse: collapse; }
                a { color: inherit; text-decoration: none; }
              </style>
            </head>
            <body style="margin:0;padding:0;background:#f4f6f8;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:8px;
                                  box-shadow:0 2px 8px rgba(0,0,0,.08);
                                  overflow:hidden;max-width:600px;">

                      <!-- Header with Gold Accent -->
                      <tr>
                        <td style="background:linear-gradient(135deg, #0b0c0e 0%%, #13151a 100%%);
                                   padding:32px 40px;text-align:center;border-bottom:3px solid #c9a84c;">
                          <h1 style="color:#c9a84c;margin:0;font-size:28px;font-weight:300;
                                     letter-spacing:2px;">
                            AJ INVESTMENT
                          </h1>
                          <p style="color:#e2ddd4;margin:8px 0 0;font-size:12px;letter-spacing:1px;">
                            Client Account Verification
                          </p>
                        </td>
                      </tr>

                      <!-- Body Content -->
                      <tr>
                        <td style="padding:40px;">
                          <h2 style="color:#0f172a;margin:0 0 16px;font-size:20px;font-weight:600;">
                            Welcome, %s!
                          </h2>
                          <p style="color:#374151;line-height:1.6;margin:0 0 24px;font-size:14px;">
                            Thank you for registering with <strong>AJ Investment</strong>.
                            To complete your registration and activate your client account,
                            please verify your email address by clicking the button below.
                          </p>

                          <!-- CTA Button -->
                          <table cellpadding="0" cellspacing="0" width="100%%">
                            <tr>
                              <td align="center" style="padding:8px 0 32px;">
                                <a href="%s"
                                   style="display:inline-block;
                                          background:#c9a84c;
                                          color:#0b0c0e;
                                          text-decoration:none;
                                          padding:14px 32px;
                                          border-radius:6px;
                                          font-size:16px;
                                          font-weight:600;
                                          letter-spacing:1px;">
                                  âœ“ VERIFY MY EMAIL
                                </a>
                              </td>
                            </tr>
                          </table>

                          <p style="color:#6b7280;font-size:13px;line-height:1.6;margin:0 0 12px;border-top:1px solid #e5e7eb;padding-top:16px;">
                            If the button doesn't work, copy and paste this link into your browser:
                          </p>
                          <p style="word-break:break-all;font-size:12px;margin:0 0 24px;">
                            <a href="%s" style="color:#c9a84c;text-decoration:underline;">%s</a>
                          </p>

                          <!-- Info Box -->
                          <div style="background:#f8fafc;border-left:3px solid #c9a84c;padding:12px 16px;margin:20px 0;">
                            <p style="color:#374151;font-size:12px;margin:0;line-height:1.5;">
                              <strong>â± Link expires in 24 hours.</strong> Once verified, you'll have full access to your AJ Investment client portal.
                            </p>
                          </div>

                          <p style="color:#9ca3af;font-size:12px;margin:16px 0 0;">
                            If you did not create this account, please disregard this email.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f8fafc;padding:20px 40px;text-align:center;
                                   border-top:1px solid #e5e7eb;">
                          <p style="color:#9ca3af;font-size:12px;margin:0;">
                            &copy; 2026 AJ Investment &nbsp;|&nbsp;
                            <a href="mailto:support@ajinvestment.co.za"
                               style="color:#6b7280;text-decoration:none;">
                              support@ajinvestment.co.za
                            </a>
                          </p>
                          <p style="color:#9ca3af;font-size:11px;margin:8px 0 0;">
                            Investment Group &nbsp;â€¢&nbsp; Client Registration
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(firstname, verifyUrl, verifyUrl, verifyUrl);
    }
    /**
     * Send password reset link to an existing client.
     */
    public void sendPasswordResetEmail(String toEmail,
                                       String firstname,
                                       String token,
                                       String resetUrl)
            throws MessagingException {

        LOGGER.info("EMAIL SERVICE: Sending password reset email");
        LOGGER.info("To: " + toEmail);
        LOGGER.info("Token: " + token);
        LOGGER.info("Reset URL: " + resetUrl);

        Session mailSession = buildSession();
        MimeMessage msg = new MimeMessage(mailSession);

        try {
            msg.setFrom(new InternetAddress(FROM_ADDRESS, FROM_NAME, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            msg.setFrom(new InternetAddress(FROM_ADDRESS));
        }

        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        msg.setSubject("Reset your AJ Investment Password");
        msg.setContent(buildPasswordResetHtmlBody(firstname, resetUrl), "text/html;charset=UTF-8");

        Transport.send(msg);
        LOGGER.info("Password reset email sent successfully");
    }

    private String buildPasswordResetHtmlBody(String firstname, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Reset your AJ Investment Password</title>
              <style>
                body { margin: 0; padding: 0; background: #f4f6f8; font-family: Arial, sans-serif; }
                table { border-collapse: collapse; }
                a { color: inherit; text-decoration: none; }
              </style>
            </head>
            <body style="margin:0;padding:0;background:#f4f6f8;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,.08);overflow:hidden;max-width:600px;">
                      <tr>
                        <td style="background:linear-gradient(135deg, #0b0c0e 0%%, #13151a 100%%);padding:32px 40px;text-align:center;border-bottom:3px solid #c9a84c;">
                          <h1 style="color:#c9a84c;margin:0;font-size:28px;font-weight:300;letter-spacing:2px;">AJ INVESTMENT</h1>
                          <p style="color:#e2ddd4;margin:8px 0 0;font-size:12px;letter-spacing:1px;">Password Reset Request</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:40px;">
                          <h2 style="color:#0f172a;margin:0 0 16px;font-size:20px;font-weight:600;">Hello, %s</h2>
                          <p style="color:#374151;line-height:1.6;margin:0 0 24px;font-size:14px;">
                            We received a request to reset the password for your AJ Investment account.
                            Click the button below to create a new password.
                          </p>
                          <table cellpadding="0" cellspacing="0" width="100%%">
                            <tr>
                              <td align="center" style="padding:8px 0 32px;">
                                <a href="%s" style="display:inline-block;background:#c9a84c;color:#0b0c0e;text-decoration:none;padding:14px 32px;border-radius:6px;font-size:16px;font-weight:600;letter-spacing:1px;">
                                  RESET PASSWORD
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="color:#6b7280;font-size:13px;line-height:1.6;margin:0 0 12px;border-top:1px solid #e5e7eb;padding-top:16px;">
                            If the button does not work, copy and paste this link into your browser:
                          </p>
                          <p style="word-break:break-all;font-size:12px;margin:0 0 24px;">
                            <a href="%s" style="color:#c9a84c;text-decoration:underline;">%s</a>
                          </p>
                          <div style="background:#f8fafc;border-left:3px solid #c9a84c;padding:12px 16px;margin:20px 0;">
                            <p style="color:#374151;font-size:12px;margin:0;line-height:1.5;"><strong>This link expires in 1 hour.</strong></p>
                          </div>
                          <p style="color:#9ca3af;font-size:12px;margin:16px 0 0;">If you did not request this password reset, please ignore this email.</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e5e7eb;">
                          <p style="color:#9ca3af;font-size:12px;margin:0;">&copy; 2026 AJ Investment &nbsp;|&nbsp; <a href="mailto:support@ajinvestment.co.za" style="color:#6b7280;text-decoration:none;">support@ajinvestment.co.za</a></p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(firstname == null || firstname.isBlank() ? "Client" : firstname, resetUrl, resetUrl, resetUrl);
    }
}

