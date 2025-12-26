// CourtCase.java
package com.CourtAssist.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "court_cases")
@Getter
@Setter
public class CourtCase {
    @Id
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "court_level", nullable = false)
    private CourtLevel courtLevel;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String district;

    @Column(name = "court_complex", nullable = false)
    private String courtComplex;

    @Column(name = "court_name")
    private String courtName;

    @Column(name = "case_type", nullable = false)
    private String caseType;

    @Column(name = "case_no", nullable = false)
    private String caseNo;

    @Column(name = "case_year", nullable = false)
    private Integer caseYear;

    @Column(name = "petitioner_names", columnDefinition = "TEXT")
    private String petitionerNames;

    @Column(name = "respondent_names", columnDefinition = "TEXT")
    private String respondentNames;

    @Column(name = "petitioner_advocate_names", columnDefinition = "TEXT")
    private String petitionerAdvocateNames;

    @Column(name = "respondent_advocate_names", columnDefinition = "TEXT")
    private String respondentAdvocateNames;

    @ManyToOne
    @JoinColumn(name = "parent_case_id")
    private CourtCase parentCase;

    @OneToMany(mappedBy = "parentCase")
    private List<CourtCase> childCases;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "courtCase")
    private List<CourtHearing> hearings;

    public enum CourtLevel {
        SUPREME_COURT, HIGH_COURT, DISTRICT_COURT, TALUK_COURT
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}