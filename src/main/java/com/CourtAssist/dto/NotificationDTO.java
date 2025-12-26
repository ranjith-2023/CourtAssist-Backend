package com.CourtAssist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String hearingId;
    private String caseRef;
    private LocalDate hearingDate;
    private LocalTime hearingTime;
    private String court;
    private String stage;
    private String parties;
    private String advocates;
    private Boolean isRead;
    private Boolean isSent;
    private LocalDateTime createdAt;
}
