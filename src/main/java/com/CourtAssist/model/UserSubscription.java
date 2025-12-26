// UserSubscription.java
package com.CourtAssist.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Getter
@Setter
public class UserSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "court_level")
    private CourtCase.CourtLevel courtLevel;

    private String state;
    private String district;

    @Column(name = "court_complex")
    private String courtComplex;

    @Column(name = "court_name")
    private String courtName;

    @Column(name = "case_type")
    private String caseType;

    @Column(name = "case_no")
    private String caseNo;

    @Column(name = "case_year")
    private Integer caseYear;

    @Column(name = "advocate_name")
    private String advocateName;

    @Column(name = "litigant_name")
    private String litigantName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}