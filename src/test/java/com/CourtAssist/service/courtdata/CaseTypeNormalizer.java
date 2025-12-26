package com.CourtAssist.service.courtdata;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

@Component
public class CaseTypeNormalizer {

    private final Map<String, String> caseTypeMapping;

    public CaseTypeNormalizer() {
        this.caseTypeMapping = new HashMap<>();
        initializeMapping();
    }

    private void initializeMapping() {
        // Map raw API case types to user-friendly normalized names
        // Review Applications
        caseTypeMapping.put("REV.APLW(MD)", "Review Applications");
        caseTypeMapping.put("REV.APLC(MD)", "Review Applications");

        // Civil Appeals
        caseTypeMapping.put("CMA(MD)", "Civil Appeals");
        caseTypeMapping.put("SA(MD)", "Civil Appeals");
        caseTypeMapping.put("SA", "Civil Appeals");

        // Criminal Appeals
        caseTypeMapping.put("CRL A(MD)", "Criminal Appeals");
        caseTypeMapping.put("CRL A(MD)(F)", "Criminal Appeals");

        // Criminal Original Proceedings
        caseTypeMapping.put("CRL OP(MD)", "Criminal Original Petitions");
        caseTypeMapping.put("CRL OP(MD)(F)", "Criminal Original Petitions");

        // Civil Miscellaneous
        caseTypeMapping.put("CMP(MD)", "Civil Miscellaneous Petitions");
        caseTypeMapping.put("WMP(MD)", "Civil Miscellaneous Petitions");

        // Criminal Miscellaneous
        caseTypeMapping.put("CRL MP(MD)", "Criminal Miscellaneous Petitions");

        // Writ Proceedings
        caseTypeMapping.put("WP(MD)", "Writ Petitions");

        // Revision Applications
        caseTypeMapping.put("CRP(MD)", "Revision Applications");

        // Contempt Proceedings
        caseTypeMapping.put("CONT P(MD)", "Contempt Petitions");

        // Subsidiary Applications
        caseTypeMapping.put("SUB A(MD)", "Subsidiary Applications");
    }

    public String normalizeCaseType(String rawCaseType) {
        if (rawCaseType == null || rawCaseType.trim().isEmpty()) {
            return "Unknown";
        }
        return caseTypeMapping.getOrDefault(rawCaseType, "Unknown");
    }
}