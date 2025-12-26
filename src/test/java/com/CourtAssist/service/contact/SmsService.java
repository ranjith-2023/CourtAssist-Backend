package com.CourtAssist.service.contact;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    // Initialize Twilio when the service is created
    public SmsService(
            @Value("${twilio.account.sid}") String accountSid,
            @Value("${twilio.auth.token}") String authToken) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        Twilio.init(accountSid, authToken);
    }

    public void sendSMS(String toMobileNo, String text) {
        try {
            // Ensure the phone number has the correct format
            String formattedNumber = formatPhoneNumber(toMobileNo);

            Message message = Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    text
            ).create();

            System.out.println("SMS sent successfully to: " + formattedNumber);
            System.out.println("Message SID: " + message.getSid());
        } catch (Exception e) {
            System.err.println("Failed to send SMS to " + toMobileNo + ": " + e.getMessage());
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove any non-digit characters
        String digitsOnly = phoneNumber.replaceAll("\\D+", "");

        // If the number doesn't start with a country code, add +91 for India
        if (digitsOnly.startsWith("+")) {
            return digitsOnly;
        } else if (digitsOnly.length() == 10) {
            return "+91" + digitsOnly;
        } else if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        } else {
            return "+91" + digitsOnly; // Default to India code
        }
    }
}