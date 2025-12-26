// Notification.java
package com.CourtAssist.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CourtCase courtCase;

    @Column(name = "hearing_id", nullable = false)
    private String hearingId;

    @Column(name = "case_ref", nullable = false)
    private String caseRef;

    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @Column(name = "hearing_time", nullable = false)
    private LocalTime hearingTime;

    @Column(nullable = false)
    private String court;

    @Column(nullable = false)
    private String stage;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String parties;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String advocates;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "is_sent")
    private Boolean isSent = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}