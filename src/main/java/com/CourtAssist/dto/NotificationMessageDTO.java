// NotificationDTO.java
package com.CourtAssist.dto;

import com.CourtAssist.model.CourtCase;
import com.CourtAssist.model.CourtHearing;
import com.CourtAssist.model.UserSubscription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
public class NotificationMessageDTO {
    private final String caseRef;
    private final LocalDateTime hearingDateTime;
    private final String court;
    private final String stage;
    private final String hearingId;
    private final String parties;
    private final String advocates;
    private final String subscriptionType;
    private final String formattedMessage;

    public NotificationMessageDTO(String caseRef, LocalDateTime hearingDateTime, String court,
                                  String stage, String hearingId, String parties, String advocates,
                                  String subscriptionType) {
        this.caseRef = caseRef;
        this.hearingDateTime = hearingDateTime;
        this.court = court;
        this.stage = stage;
        this.hearingId = hearingId;
        this.parties = parties;
        this.advocates = advocates;
        this.subscriptionType = subscriptionType;
        //change this in desired format
        this.formattedMessage = generateFormattedHtmlMessage();
    }

    public static NotificationMessageDTO createFrom(CourtCase courtCase, CourtHearing hearing,
                                                    UserSubscription subscription) {
        String caseRef = courtCase.getCaseNo() + "/" + courtCase.getCaseYear();
        String parties = formatParties(courtCase.getPetitionerNames(), courtCase.getRespondentNames());
        String advocates = formatAdvocates(courtCase.getPetitionerAdvocateNames(), courtCase.getRespondentAdvocateNames());
        String subType = subscription.getAdvocateName() != null ? "ADVOCATE" :
                subscription.getLitigantName() != null ? "LITIGANT" : "CASE";

        return new NotificationMessageDTO(
                caseRef,
                hearing.getHearingDatetime(),
                courtCase.getCourtComplex(),
                hearing.getStage(),
                hearing.getHearingId(),
                parties,
                advocates,
                subType
        );
    }

    private static String formatParties(String petitionerNames, String respondentNames) {
        StringBuilder sb = new StringBuilder();

        if (isValidName(petitionerNames)) {
            sb.append("Petitioner: ").append(cleanAndTruncate(petitionerNames, 50));
        }

        if (isValidName(respondentNames)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Respondent: ").append(cleanAndTruncate(respondentNames, 50));
        }

        return sb.length() > 0 ? sb.toString() : "Parties information not available";
    }

    private static String formatAdvocates(String petitionerAdvocates, String respondentAdvocates) {
        StringBuilder sb = new StringBuilder();

        if (isValidName(petitionerAdvocates)) {
            sb.append("Pet Advocate: ").append(cleanAndTruncate(petitionerAdvocates, 50));
        }

        if (isValidName(respondentAdvocates)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Res Advocate: ").append(cleanAndTruncate(respondentAdvocates, 50));
        }

        return sb.length() > 0 ? sb.toString() : "Advocate information not available";
    }

    private String generateFormattedMessage() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        return String.format(
                "🏛️ Court Hearing Notification (%s)\n\n" +
                        "📋 Case: %s\n" +
                        "📅 Date: %s\n" +
                        "⏰ Time: %s\n" +
                        "⚖️ Court: %s\n" +
                        "📝 Stage: %s\n\n" +
                        "👥 Parties:\n%s\n\n" +
                        "⚖️ Advocates:\n%s\n\n" +
                        "---\n" +
                        "This is an automated notification from CourtAssist",
                subscriptionType,
                caseRef,
                hearingDateTime.format(dateFormatter),
                hearingDateTime.format(timeFormatter),
                court,
                stage,
                parties,
                advocates
        );
    }

    private String generateFormattedHtmlMessage() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<body style='font-family: sans-serif; background-color: #f0f2f5; padding: 20px; margin: 0;'>" +
                        "    <div style='max-width: 600px; margin: 0 auto; background-color: #0b1a2e; border-radius: 12px; overflow: hidden; border: 1px solid #D4AF37;'>" +
                        "        <div style='background-color: #D4AF37; padding: 25px; text-align: center;'>" +
                        "            <h1 style='margin: 0; color: #0b1a2e; font-size: 22px;'>🏛️ Court Hearing Notification</h1>" +
                        "            <p style='margin: 5px 0 0 0; color: #0b1a2e; font-weight: bold; opacity: 0.8;'>(%s)</p>" +
                        "        </div>" +
                        "        <div style='padding: 30px; color: #ffffff;'>" +
                        "            <p style='margin: 12px 0;'><strong style='color: #D4AF37; display: inline-block; width: 100px;'>📋 Case:</strong> %s</p>" +
                        "            <p style='margin: 12px 0;'><strong style='color: #D4AF37; display: inline-block; width: 100px;'>📅 Date:</strong> %s</p>" +
                        "            <p style='margin: 12px 0;'><strong style='color: #D4AF37; display: inline-block; width: 100px;'>⏰ Time:</strong> %s</p>" +
                        "            <p style='margin: 12px 0;'><strong style='color: #D4AF37; display: inline-block; width: 100px;'>⚖️ Court:</strong> %s</p>" +
                        "            <p style='margin: 12px 0;'><strong style='color: #D4AF37; display: inline-block; width: 100px;'>📝 Stage:</strong> %s</p>" +
                        "            <hr style='border: 0; border-top: 1px solid rgba(212, 175, 55, 0.3); margin: 25px 0;'>" +
                        "            <h3 style='color: #D4AF37; margin-bottom: 10px; font-size: 18px;'>👥 Parties</h3>" +
                        "            <div style='background-color: rgba(255,255,255,0.05); padding: 15px; border-radius: 6px; border-left: 3px solid #D4AF37; white-space: pre-wrap;'>%s</div>" +
                        "            <h3 style='color: #D4AF37; margin: 20px 0 10px 0; font-size: 18px;'>⚖️ Advocates</h3>" +
                        "            <div style='background-color: rgba(255,255,255,0.05); padding: 15px; border-radius: 6px; border-left: 3px solid #D4AF37; white-space: pre-wrap;'>%s</div>" +
                        "        </div>" +
                        "        <div style='background-color: rgba(0,0,0,0.3); padding: 20px; text-align: center; color: #8892b0; font-size: 12px; border-top: 1px solid rgba(212, 175, 55, 0.2);'>" +
                        "            This is an automated notification from <strong>CourtAssist</strong>" +
                        "        </div>" +
                        "    </div>" +
                        "</body>" +
                        "</html>",
                subscriptionType,
                caseRef,
                hearingDateTime.format(dateFormatter),
                hearingDateTime.format(timeFormatter),
                court,
                stage,
                parties,
                advocates
        );
    }

    private static String cleanAndTruncate(String text, int maxLength) {
        if (!isValidName(text)) return "Not Available";
        String cleaned = text.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength - 3) + "...";
    }

    private static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && !"Not Available".equalsIgnoreCase(name.trim());
    }
}