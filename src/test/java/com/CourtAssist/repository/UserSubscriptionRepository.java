package com.CourtAssist.repository;

import com.CourtAssist.model.CourtCase;
import com.CourtAssist.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    @Query("SELECT us FROM UserSubscription us WHERE " +
            "(:courtLevel IS NULL OR us.courtLevel = :courtLevel) AND " +
            "(:state IS NULL OR us.state = :state) AND " +
            "(:district IS NULL OR us.district = :district) AND " +
            "(:courtComplex IS NULL OR us.courtComplex = :courtComplex) AND " +
            "(:caseType IS NULL OR us.caseType = :caseType) AND " +
            "(:caseNo IS NULL OR us.caseNo = :caseNo) AND " +
            "(:caseYear IS NULL OR us.caseYear = :caseYear)")
    List<UserSubscription> findMatchingSubscriptions(
            @Param("courtLevel") CourtCase.CourtLevel courtLevel,
            @Param("state") String state,
            @Param("district") String district,
            @Param("courtComplex") String courtComplex,
            @Param("caseType") String caseType,
            @Param("caseNo") String caseNo,
            @Param("caseYear") Integer caseYear
    );

    @Query("SELECT us FROM UserSubscription us WHERE " +
            "us.user.userId = :userId")
    List<UserSubscription> findByUserId(@Param("userId")Long userId);

    @Query("SELECT DISTINCT us FROM UserSubscription us WHERE " +
            "us.advocateName IS NOT NULL OR us.litigantName IS NOT NULL")
    List<UserSubscription> findNameBasedSubscriptions();
}