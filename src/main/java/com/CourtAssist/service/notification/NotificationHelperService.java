// NotificationHelperService.java
package com.CourtAssist.service.notification;

import com.CourtAssist.dto.NotificationMessageDTO;
import com.CourtAssist.service.contact.EmailService;
import com.CourtAssist.service.contact.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationHelperService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationHelperService.class);

    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationHelperService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public void sendHearingNotification(String email, String mobile, NotificationMessageDTO dto) {
        int sentCount = 0;

        if (StringUtils.hasText(email)) {
            try {
                String subject = "Court Hearing Alert: " + dto.getCaseRef();
                String body = dto.getFormattedMessage();
                emailService.sendMail(email, subject, body);
                sentCount++;
                logger.debug("Email sent to {} for case {}", email, dto.getCaseRef());
            } catch (Exception e) {
                logger.error("Failed to send email to {}: {}", email, e.getMessage());
            }
        }

        if (StringUtils.hasText(mobile)) {
            try {
                String smsMessage = formatSmsMessage(dto);
                smsService.sendSMS(mobile, smsMessage);
                sentCount++;
                logger.debug("SMS sent to {} for case {}", mobile, dto.getCaseRef());
            } catch (Exception e) {
                logger.error("Failed to send SMS to {}: {}", mobile, e.getMessage());
            }
        }

        if (sentCount == 0) {
            logger.warn("No notifications sent for case {} - no valid contact", dto.getCaseRef());
        }
    }

    private String formatSmsMessage(NotificationMessageDTO dto) {
        return String.format(
                "Hearing: %s on %s at %s. Court: %s. %s",
                dto.getCaseRef(),
                dto.getHearingDateTime().toLocalDate(),
                dto.getHearingDateTime().toLocalTime(),
                abbreviate(dto.getCourt(), 25),
                abbreviate(dto.getStage(), 20)
        );
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}