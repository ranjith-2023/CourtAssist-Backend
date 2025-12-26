// CourtHearing.java
package com.CourtAssist.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "court_hearings")
@Getter
@Setter
public class CourtHearing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hearing_id", unique = true, nullable = false)
    private String hearingId;

    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CourtCase courtCase;

    @Column(name = "court_no", nullable = false)
    private String courtNo;

    @Column(nullable = false)
    private String stage;

    @Column(name = "hearing_datetime", nullable = false)
    private LocalDateTime hearingDatetime;

    @Column(name = "court_remarks", columnDefinition = "TEXT")
    private String courtRemarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}