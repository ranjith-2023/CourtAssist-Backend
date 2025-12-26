// NameMatchingService.java
package com.CourtAssist.service.notification;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class NameMatchingService {
    private static final Logger logger = LoggerFactory.getLogger(NameMatchingService.class);

    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final Set<String> LEGAL_NOISE_WORDS = Set.of(
            "m/s", "m/s.", "memorandum", "memo", "filed", "by", "special", "government",
            "pleader", "cgc", "court", "notice", "public", "prosecutor", "additional",
            "addl", "learned", "advocate", "for", "r1", "r2", "usr", "dt", "the", "and"
    );

    private static final Pattern NAME_DELIMITERS = Pattern.compile("[,\\s&.]+");
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    /**
     * Enhanced name matching that handles partial matches and noisy data
     * Example: 'DhanaSekaran' matches 'B. DHANASEKARAN' in 'DR.R.ALAGUMANI S.RAMESH KUMARMS,2757,2012,B. DHANASEKARAN...'
     */
    public boolean matchesAnyName(String searchName, String... nameFields) {
        if (!StringUtils.hasText(searchName)) {
            logger.debug("Empty search name provided");
            return false;
        }

        String normalizedSearch = normalizeSearchName(searchName);
        if (normalizedSearch.isEmpty()) {
            logger.debug("Search name normalized to empty: '{}'", searchName);
            return false;
        }

        logger.debug("NAME SEARCH: Looking for '{}' (normalized: '{}')", searchName, normalizedSearch);

        for (String nameField : nameFields) {
            if (matchesSingleName(normalizedSearch, nameField)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesSingleName(String searchName, String targetName) {
        if (!StringUtils.hasText(targetName)) return false;

        String normalizedTarget = normalizeTargetName(targetName);
        if (normalizedTarget.isEmpty()) return false;

        // Strategy 1: Direct containment check
        if (normalizedTarget.contains(searchName)) {
            logger.debug("DIRECT CONTAINMENT: '{}' contains '{}'", normalizedTarget, searchName);
            return true;
        }

        // Strategy 2: Token-based matching
        Set<String> searchTokens = extractNameTokens(searchName);
        Set<String> targetTokens = extractNameTokens(normalizedTarget);

        for (String searchToken : searchTokens) {
            for (String targetToken : targetTokens) {
                if (isTokenMatch(searchToken, targetToken)) {
                    logger.debug("TOKEN MATCH: '{}' matches '{}'", searchToken, targetToken);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Normalize search name (from subscription)
     */
    private String normalizeSearchName(String name) {
        if (!StringUtils.hasText(name)) return "";

        return name.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Normalize target name (from court data) - more aggressive cleaning
     */
    private String normalizeTargetName(String name) {
        if (!StringUtils.hasText(name)) return "";

        String normalized = name.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();

        // Remove common legal procedural text
        for (String noiseWord : LEGAL_NOISE_WORDS) {
            normalized = normalized.replaceAll("\\b" + noiseWord + "\\b", "");
        }

        return normalized.replaceAll("\\s+", " ").trim();
    }

    private Set<String> extractNameTokens(String name) {
        Set<String> tokens = new HashSet<>();
        if (!StringUtils.hasText(name)) return tokens;

        String[] parts = NAME_DELIMITERS.split(name.toLowerCase());

        for (String part : parts) {
            String cleaned = part.trim();
            if (cleaned.length() > 2 && !LEGAL_NOISE_WORDS.contains(cleaned)) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }

    private boolean isTokenMatch(String token1, String token2) {
        if (token1.equals(token2)) return true;

        // Containment check
        if (token1.contains(token2) || token2.contains(token1)) return true;

        // Fuzzy similarity check
        int distance = levenshtein.apply(token1, token2);
        double maxLength = Math.max(token1.length(), token2.length());
        double similarity = 1.0 - (distance / maxLength);

        return similarity >= SIMILARITY_THRESHOLD;
    }
}