package com.CourtAssist.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.base64:}")
    private String firebaseConfigBase64;

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;

            if (firebaseConfigBase64 != null && !firebaseConfigBase64.isEmpty()) {
                // Use Base64 from environment variable (for deployment)
                byte[] decodedBytes = Base64.getDecoder().decode(firebaseConfigBase64);
                serviceAccount = new ByteArrayInputStream(decodedBytes);
                System.out.println("Using Firebase config from environment variable");
            } else {
                // Use local file (for development)
                serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
                System.out.println("Using Firebase config from local file");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }
}