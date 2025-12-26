package com.CourtAssist.controller;

import com.CourtAssist.service.contact.EmailService;
import com.CourtAssist.service.contact.SmsService;
import com.CourtAssist.util.OtpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/user")
public class OtpController {

    private final OtpService otpService;
    private final EmailService emailService;
    private final SmsService smsService;

    // In-memory storage for OTPs (use Redis in production)
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    public OtpController(OtpService otpService, EmailService emailService, SmsService smsService) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @PostMapping("/send-contact-verification-otp")
    public ResponseEntity<?> sendContactVerificationOtp(@RequestBody Map<String, String> request) {
        try {
            String contact = request.get("contact");
            if (contact == null || contact.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Contact is required"));
            }

            // Generate OTP
            String otp = otpService.generateOtp();

            // Store OTP with expiry (10 minutes)
            String normalizedContact = normalizeContact(contact);
            otpStorage.put(normalizedContact, otp);
            otpExpiry.put(normalizedContact, System.currentTimeMillis() + 10 * 60 * 1000);

            // Send OTP via appropriate channel
            if (contact.contains("@")) {
                // Email
                String subject = "Contact Verification OTP";
                String body = generateOtpHtml("Contact Verification", otp, "10");
                emailService.sendMail(contact, subject, body);
            } else {
                // SMS
                String message = "Court Assist - Your verification OTP is: " + otp + ". Valid for 10 minutes.";
                smsService.sendSMS(contact, message);
            }

            return ResponseEntity.ok().body(Map.of("message", "OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-contact-verification-otp")
    public ResponseEntity<?> verifyContactVerificationOtp(@RequestBody Map<String, String> request) {
        try {
            String contact = request.get("contact");
            String otp = request.get("otp");

            if (contact == null || otp == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Contact and OTP are required"));
            }

            String normalizedContact = normalizeContact(contact);
            String storedOtp = otpStorage.get(normalizedContact);
            Long expiryTime = otpExpiry.get(normalizedContact);

            if (storedOtp == null || expiryTime == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "OTP not found or expired"));
            }

            if (System.currentTimeMillis() > expiryTime) {
                // Clean up expired OTP
                otpStorage.remove(normalizedContact);
                otpExpiry.remove(normalizedContact);
                return ResponseEntity.badRequest().body(Map.of("error", "OTP has expired"));
            }

            if (!storedOtp.equals(otp)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
            }

            // OTP verified successfully - clean up
            otpStorage.remove(normalizedContact);
            otpExpiry.remove(normalizedContact);

            return ResponseEntity.ok().body(Map.of("message", "Contact verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification failed: " + e.getMessage()));
        }
    }

    // Add password reset OTP endpoints as well
    @PostMapping("/send-password-reset-otp")
    public ResponseEntity<?> sendPasswordResetOtp(@RequestBody Map<String, String> request) {
        // Similar implementation to contact verification
        try {
            String contact = request.get("contact");
            if (contact == null || contact.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Contact is required"));
            }

            String otp = otpService.generateOtp();
            String normalizedContact = normalizeContact(contact);
            otpStorage.put(normalizedContact, otp);
            otpExpiry.put(normalizedContact, System.currentTimeMillis() + 10 * 60 * 1000);

            if (contact.contains("@")) {
                String subject = "Password Reset OTP";
                String body = generateOtpHtml("Password Reset", otp, "10");
                emailService.sendMail(contact, subject, body);
            } else {
                String message = "Your password reset OTP is: " + otp + ". Valid for 10 minutes.";
                smsService.sendSMS(contact, message);
            }

            return ResponseEntity.ok().body(Map.of("message", "Password reset OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-password-reset-otp")
    public ResponseEntity<?> verifyPasswordResetOtp(@RequestBody Map<String, String> request) {
        // Similar to contact verification verification
        try {
            String contact = request.get("contact");
            String otp = request.get("otp");

            if (contact == null || otp == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Contact and OTP are required"));
            }

            String normalizedContact = normalizeContact(contact);
            String storedOtp = otpStorage.get(normalizedContact);
            Long expiryTime = otpExpiry.get(normalizedContact);

            if (storedOtp == null || expiryTime == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "OTP not found or expired"));
            }

            if (System.currentTimeMillis() > expiryTime) {
                otpStorage.remove(normalizedContact);
                otpExpiry.remove(normalizedContact);
                return ResponseEntity.badRequest().body(Map.of("error", "OTP has expired"));
            }

            if (!storedOtp.equals(otp)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
            }

            // Mark this OTP as used for password reset
            return ResponseEntity.ok().body(Map.of("message", "OTP verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification failed: " + e.getMessage()));
        }
    }

    private String normalizeContact(String contact) {
        return contact.trim().toLowerCase();
    }

    private String generateOtpHtml(String scenarioTitle, String otp, String expiryMinutes) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<body style='font-family: sans-serif; background-color: #f0f2f5; padding: 40px 20px; margin: 0;'>" +
                        "    <div style='max-width: 500px; margin: 0 auto; background-color: #0b1a2e; border-radius: 12px; overflow: hidden; border: 1px solid #D4AF37; box-shadow: 0 10px 25px rgba(0,0,0,0.2);'>" +
                        "        " +
                        "        " +
                        "        <div style='background-color: #D4AF37; padding: 30px 20px; text-align: center;'>" +
                        "            <h1 style='margin: 0; color: #0b1a2e; font-size: 22px; text-transform: uppercase; letter-spacing: 1px;'>" +
                        "                🏛️ CourtAssist Security" +
                        "            </h1>" +
                        "            <p style='margin: 5px 0 0 0; color: #0b1a2e; font-weight: bold; opacity: 0.8;'>" +
                        "                (%s)" + // Scenario Title: e.g., Password Reset
                        "            </p>" +
                        "        </div>" +
                        "        " +
                        "        " +
                        "        <div style='padding: 30px; color: #ffffff; text-align: center;'>" +
                        "            <p style='font-size: 16px; color: #cbd5e1;'>Please use the following One-Time Password (OTP) to complete your request.</p>" +
                        "            " +
                        "            " +
                        "            <div style='background-color: rgba(255,255,255,0.05); padding: 25px; margin: 25px 0; border-radius: 6px; border-left: 4px solid #D4AF37; border-right: 4px solid #D4AF37;'>" +
                        "                <span style='font-size: 40px; font-weight: bold; color: #D4AF37; letter-spacing: 12px; font-family: monospace;'>%s</span>" +
                        "            </div>" +
                        "            " +
                        "            <p style='font-size: 14px; color: #94a3b8; line-height: 1.6;'>" +
                        "                This code is valid for <strong style='color: #D4AF37;'>%s minutes</strong>.<br>" +
                        "                For your protection, do not share this code with anyone." +
                        "            </p>" +
                        "            " +
                        "            <hr style='border: 0; border-top: 1px solid rgba(212, 175, 55, 0.3); margin: 25px 0;'>" +
                        "            " +
                        "            <p style='font-size: 12px; color: #64748b;'>" +
                        "                If you did not request this code, please secure your account or contact support immediately." +
                        "            </p>" +
                        "        </div>" +
                        "        " +
                        "        " +
                        "        <div style='background-color: rgba(0,0,0,0.3); padding: 20px; text-align: center; color: #8892b0; font-size: 12px; border-top: 1px solid rgba(212, 175, 55, 0.2);'>" +
                        "            This is an automated security notification from <strong>CourtAssist</strong>" +
                        "        </div>" +
                        "    </div>" +
                        "</body>" +
                        "</html>",
                scenarioTitle,
                otp,
                expiryMinutes
        );
    }
}