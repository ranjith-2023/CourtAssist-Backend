package com.CourtAssist.service.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmService {
    Logger logger = LoggerFactory.getLogger(FcmService.class);
    public void sendNotificationToToken(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.trim().isEmpty()) {
            logger.warn("Skipping FCM notification: empty token for title: {}", title);
            return;
        }
        // Build the notification message
        Message message = Message.builder()
                .setToken(targetToken) // The device token from your database
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                // Optional: Add custom data payload
                // .putData("caseRef", caseRef)
                // .putData("hearingDate", hearingDate)
                .build();

        try {
            // Send the message and get the message ID
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending FCM message", e);
        }
    }
}