package com.CourtAssist.service.cleanup;

import com.CourtAssist.repository.CourtCaseRepository;
import com.CourtAssist.repository.CourtHearingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DataCleanUpService {
    private final CourtCaseRepository courtCaseRepository;
    private final CourtHearingRepository courtHearingRepository;

    public DataCleanUpService(CourtCaseRepository courtCaseRepository, CourtHearingRepository courtHearingRepository) {
        this.courtCaseRepository = courtCaseRepository;
        this.courtHearingRepository = courtHearingRepository;
    }

    @Transactional
    public void cleanUpData(LocalDateTime date) {
        courtCaseRepository.deleteCasesLinkedToOldHearings(date);
        courtHearingRepository.deleteHearingsBeforeDatetime(date);
    }

}
