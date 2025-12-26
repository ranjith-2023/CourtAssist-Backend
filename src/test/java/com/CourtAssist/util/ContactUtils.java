package com.CourtAssist.util;

import org.springframework.stereotype.Component;

@Component
public class ContactUtils {

    public static String normalizeContact(String contact) {
        if (contact == null) return null;

        contact = contact.trim();

        if (contact.contains("@")) {
            // Email normalization
            return contact.toLowerCase();
        } else {
            // Phone number normalization
            // Remove all non-digit characters except leading '+'
            if (contact.startsWith("+")) {
                return "+" + contact.substring(1).replaceAll("\\D", "");
            }
            return contact.replaceAll("\\D", "");
        }
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Validate mobile number format
     */
    public static boolean isValidMobileNumber(String mobileNo) {
        if (mobileNo == null) return false;
        // Basic validation - at least 10 digits
        String digitsOnly = mobileNo.replaceAll("\\D", "");
        return digitsOnly.length() >= 10;
    }
}