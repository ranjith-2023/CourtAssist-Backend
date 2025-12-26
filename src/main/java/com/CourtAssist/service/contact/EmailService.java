package com.CourtAssist.service.contact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

// uses EmailJS API (HTTPS protocol)
@Service
public class EmailService {

    private final String serviceId;
    private final String templateId;
    private final String publicKey;
    private final RestTemplate restTemplate;
    private final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();


    public EmailService(
            @Value("${emailjs.service.id}") String serviceId,
            @Value("${emailjs.template.id}") String templateId,
            @Value("${emailjs.public.key}") String publicKey) {
        this.serviceId = serviceId;
        this.templateId = templateId;
        this.publicKey = publicKey;
        this.factory.setConnectTimeout(10000);
        this.factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }



    public void sendMail(String to, String subject, String bodyHtml) {

        String url = "https://api.emailjs.com/api/v1.0/email/send";

        // 1. Set Headers explicitly
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Prepare Template Params
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("to_email", to);
        templateParams.put("subject", subject);
        templateParams.put("content_html", bodyHtml);

        // 3. Prepare Payload (Keys must be exact)
        Map<String, Object> payload = new HashMap<>();
        payload.put("service_id", serviceId);
        payload.put("template_id", templateId);
        payload.put("user_id", publicKey);
        payload.put("template_params", templateParams);

        // 4. Wrap in HttpEntity
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
            System.out.println("Email sent successfully to: " + to);
        } catch (Exception e) {
            // Detailed error logging
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}

/**
 * Uses SMTP Protocol
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}
 **/