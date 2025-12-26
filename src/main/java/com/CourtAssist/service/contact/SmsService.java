package com.CourtAssist.service.contact;

// Using SMS Gateway
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {

    // Use the base URL: https://api.sms-gate.app
    @Value("${smsgate.url}")
    private String gatewayUrl;

    @Value("${smsgate.username}")
    private String username;

    @Value("${smsgate.password}")
    private String password;

    @Value("${smsgate.device.id}")
    private String deviceId;

    private final RestTemplate restTemplate;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    @Async
    public void sendSMS(String toMobileNo, String text) {
        try {
            String formattedNumber = formatPhoneNumber(toMobileNo);

            // Correct 3rd party endpoint for sending messages
            String endpoint = gatewayUrl + "/3rdparty/v1/messages";

            // Prepare the JSON body exactly as SMS Gate expects
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phoneNumbers", new String[]{formattedNumber});
            requestBody.put("deviceId", deviceId);

            Map<String, String> textMessage = new HashMap<>();
            textMessage.put("text", text);
            requestBody.put("textMessage", textMessage);

            // Set up Authentication
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(username, password);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send POST request
            restTemplate.postForEntity(endpoint, entity, String.class);

            System.out.println("SMS sent to phone queue for: " + formattedNumber);
        } catch (Exception e) {
            System.err.println("SMS Gate Error: " + e.getMessage());
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        String digitsOnly = phoneNumber.replaceAll("\\D+", "");
        if (digitsOnly.length() == 10) {
            return "+91" + digitsOnly;
        } else if (digitsOnly.startsWith("91") && digitsOnly.length() == 12) {
            return "+" + digitsOnly;
        }
        return "+" + digitsOnly.replaceFirst("^\\+", "");
    }
}

/**
 * Using Twilio API
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
 **/