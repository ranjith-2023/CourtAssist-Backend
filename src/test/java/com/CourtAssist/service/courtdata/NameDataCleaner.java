// NameDataCleaner.java
package com.CourtAssist.service.courtdata;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.text.Normalizer;

@Component
public class NameDataCleaner {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^a-zA-Z0-9\\s.,&/]");
    private static final Pattern LEGAL_PREFIXES = Pattern.compile(
            "\\b(AND|OTHERS|ANOTHER|LRS|LEGAL HEIRS|DECEASED|DIED|VIDE|TAPAL|AFFIDAVIT|REG|SR|II|BATTA|CAVEATOR)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MULTIPLE_DELIMITERS = Pattern.compile("[,&/]+");

    public String cleanNames(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "Not Available";
        }

        // Step 1: Normalize Unicode
        String cleaned = Normalizer.normalize(rawName, Normalizer.Form.NFKC);

        // Step 2: Remove legal prefixes and common noise
        cleaned = LEGAL_PREFIXES.matcher(cleaned).replaceAll("");

        // Step 3: Remove special characters but keep basic punctuation
        cleaned = SPECIAL_CHARS.matcher(cleaned).replaceAll("");

        // Step 4: Normalize delimiters
        cleaned = MULTIPLE_DELIMITERS.matcher(cleaned).replaceAll(",");

        // Step 5: Clean up whitespace
        cleaned = MULTIPLE_SPACES.matcher(cleaned).replaceAll(" ").trim();

        // Step 6: Remove trailing commas and clean up
        cleaned = cleaned.replaceAll("^,+|,+$", "").trim();

        if (cleaned.isEmpty()) {
            return "Not Available";
        }

        return cleaned;
    }
}