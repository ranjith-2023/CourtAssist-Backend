// CourtHearingRepository.java
package com.CourtAssist.repository;

import com.CourtAssist.model.CourtHearing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourtHearingRepository extends JpaRepository<CourtHearing, Long> {

    @Query("SELECT h FROM CourtHearing h WHERE h.hearingDatetime BETWEEN :start AND :end")
    List<CourtHearing> findByHearingDatetimeBetween(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    Optional<CourtHearing> findByHearingId(String hearingId);

    @Query("SELECT h FROM CourtHearing h WHERE h.courtCase.caseId = :caseId AND h.hearingDatetime = :hearingDatetime")
    Optional<CourtHearing> findByCaseAndDateTime(@Param("caseId") String caseId,
                                                 @Param("hearingDatetime") LocalDateTime hearingDatetime);

    @Query("DELETE FROM CourtHearing h WHERE h.hearingDatetime < :hearingDatetime")
    void deleteHearingsBeforeDatetime(@Param("hearingDatetime") LocalDateTime hearingDatetime);

}