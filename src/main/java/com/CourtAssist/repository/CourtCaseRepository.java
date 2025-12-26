// CourtCaseRepository.java
package com.CourtAssist.repository;

import com.CourtAssist.model.CourtCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourtCaseRepository extends JpaRepository<CourtCase, String> {

    @Query("SELECT c FROM CourtCase c WHERE " +
            "c.caseNo = :caseNo AND c.caseYear = :caseYear AND " +
            "c.courtLevel = :courtLevel AND c.state = :state AND " +
            "c.district = :district AND c.courtComplex = :courtComplex")
    Optional<CourtCase> findExistingCase(@Param("caseNo") String caseNo,
                                         @Param("caseYear") Integer caseYear,
                                         @Param("courtLevel") CourtCase.CourtLevel courtLevel,
                                         @Param("state") String state,
                                         @Param("district") String district,
                                         @Param("courtComplex") String courtComplex);

    List<CourtCase> findByParentCase(CourtCase parentCase);

    @Query("SELECT c FROM CourtCase c WHERE c.parentCase.caseId = :parentCaseId")
    List<CourtCase> findByParentCaseId(@Param("parentCaseId") String parentCaseId);

    // Add to CourtCaseRepository.java
    @Query("SELECT cc FROM CourtCase cc WHERE " +
            "cc.caseNo LIKE %:subscriptionCaseNo% AND " +
            "(cc.petitionerAdvocateNames LIKE %:subscriptionAdvocateName% OR " +
            "cc.respondentAdvocateNames LIKE %:subscriptionAdvocateName%)")
    List<CourtCase> findMatchingCases(
            @Param("subscriptionCaseNo") String subscriptionCaseNo,
            @Param("subscriptionAdvocateName") String subscriptionAdvocateName);

    @Query("DELETE FROM CourtCase cc WHERE cc.caseId IN " +
            "(SELECT h.courtCase.caseId FROM CourtHearing h WHERE h.hearingDatetime < :hearingDatetime)")
    void deleteCasesLinkedToOldHearings(@Param("hearingDatetime") LocalDateTime hearingDatetime);

}