// CourtDataImportService.java
package com.CourtAssist.service.courtdata;

import com.CourtAssist.model.CourtCase;
import com.CourtAssist.model.CourtHearing;
import com.CourtAssist.repository.CourtCaseRepository;
import com.CourtAssist.repository.CourtHearingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CourtDataImportService {
    private static final Logger logger = LoggerFactory.getLogger(CourtDataImportService.class);

    private final RestTemplate restTemplate;
    private final CourtCaseRepository courtCaseRepository;
    private final CourtHearingRepository courtHearingRepository;
    private final ObjectMapper objectMapper;
    private final NameDataCleaner nameDataCleaner;
    private final CaseTypeNormalizer caseTypeNormalizer;

    // API endpoints for High Court data
    private static final String MADURAI_HIGH_COURT_API = "https://mhc.tn.gov.in/judis/clists/clists-madurai/api/result.php";
    private static final String MADRAS_HIGH_COURT_API = "https://mhc.tn.gov.in/judis/clists/clists-madras/api/result.php";

    // Date parsing constants
    private static final Map<String, Integer> MONTH_MAP = createMonthMap();
    private static final DateTimeFormatter DATE_PARAM_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "ON\\s+\\w+\\s+THE\\s+(\\d+)(?:TH|ST|ND|RD)?\\s+DAY OF\\s+(\\w+)\\s+(\\d{4})\\s+AT\\s+(\\d{1,2})\\.(\\d{2})\\s+(A\\.M\\.|P\\.M\\.)",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * Constructor for dependency injection
     */
    public CourtDataImportService(RestTemplate restTemplate, CourtCaseRepository courtCaseRepository,
                                  CourtHearingRepository courtHearingRepository, ObjectMapper objectMapper,
                                  NameDataCleaner nameDataCleaner, CaseTypeNormalizer caseTypeNormalizer) {
        this.restTemplate = restTemplate;
        this.courtCaseRepository = courtCaseRepository;
        this.courtHearingRepository = courtHearingRepository;
        this.objectMapper = objectMapper;
        this.nameDataCleaner = nameDataCleaner;
        this.caseTypeNormalizer = caseTypeNormalizer;
    }

    /**
     * Scheduled task to import court data for the next day
     * Runs daily at 6:00 AM as configured by cron expression
     */
    @Scheduled(cron = "${court-data.import.cron:0 0 6 * * ?}")
    public String importCourtData() {
        return importCourtDataForDate(LocalDate.now().plusDays(1));
    }

    /**
     * Main method to import court data for a specific date
     *
     * @param date The date for which to import court data
     * @return Import result summary
     */
    public String importCourtDataForDate(LocalDate date) {
        logger.info("Starting court data import process for date: {}", date);
        ImportResult result = new ImportResult();

        try {
            // Import data from both High Court benches
            result.add(importHighCourtData(MADURAI_HIGH_COURT_API, "Madurai", date));
            result.add(importHighCourtData(MADRAS_HIGH_COURT_API, "Chennai", date));

            logger.info("Court data import completed successfully for {}: {}", date, result);
            return result.toString();
        } catch (Exception e) {
            logger.error("Error during court data import for {}: {}", date, e.getMessage());
            return "Failed to import court data for " + date + ": " + e.getMessage();
        }
    }

    /**
     * Imports data from a specific High Court API endpoint
     *
     * @param apiUrl The API endpoint URL
     * @param district The district name for the court
     * @param date The date for which to import data
     * @return Import result for this specific API
     */
    private ImportResult importHighCourtData(String apiUrl, String district, LocalDate date) {
        logger.info("Importing data from {} for district {} and date {}", apiUrl, district, date);
        ImportResult result = new ImportResult();

        try {
            String dateParam = date.format(DATE_PARAM_FORMATTER);
            String fullApiUrl = apiUrl + "?file=cause_" + dateParam + ".xml";

            String responseData = restTemplate.getForObject(fullApiUrl, String.class);

            if (responseData == null || responseData.trim().isEmpty()) {
                logger.info("Empty response from API for date {}", date);
                return result;
            }

            List<CourtCaseApiResponse> apiResponses = parseApiResponse(responseData);
            logger.info("Found {} cases in response from {}", apiResponses.size(), district);

            for (CourtCaseApiResponse apiResponse : apiResponses) {
                try {
                    processCourtCase(apiResponse, district);
                    result.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to process court case: {}", e.getMessage());
                    result.incrementFailed();
                }
            }

        } catch (Exception e) {
            logger.error("Failed to import data from {}: {}", apiUrl, e.getMessage());
            result.incrementFailed();
        }
        return result;
    }

    /**
     * Parses the JSON response from the court API
     *
     * @param responseData Raw JSON response string
     * @return List of parsed court case responses
     */
    private List<CourtCaseApiResponse> parseApiResponse(String responseData) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseData);
            List<CourtCaseApiResponse> responses = new ArrayList<>();

            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    responses.add(parseCourtCase(node));
                }
            } else if (rootNode.isObject()) {
                responses.add(parseCourtCase(rootNode));
            }

            return responses;
        } catch (Exception e) {
            logger.error("Failed to parse API response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parses individual court case from JSON node
     *
     * @param node JSON node containing case data
     * @return Parsed court case response
     */
    private CourtCaseApiResponse parseCourtCase(JsonNode node) {
        CourtCaseApiResponse response = new CourtCaseApiResponse();

        response.setCourtNo(getText(node, "courtno"));
        response.setCourtRemarks(getText(node, "courtremarks"));
        response.setStageName(getText(node, "stagename"));
        response.setCaseType(getText(node, "mcasetype"));
        response.setCaseNo(getText(node, "mcaseno"));
        response.setCaseYear(getText(node, "mcaseyr"));

        // Clean names during parsing to ensure data consistency
        response.setPetitionerNames(nameDataCleaner.cleanNames(getText(node, "pname")));
        response.setRespondentNames(nameDataCleaner.cleanNames(getText(node, "rname")));
        response.setPetitionerAdvocateNames(nameDataCleaner.cleanNames(getTextOrFirstFromArray(node, "mpadv")));
        response.setRespondentAdvocateNames(nameDataCleaner.cleanNames(getTextOrFirstFromArray(node, "mradv")));

        // Parse extra cases if present
        JsonNode extraNode = node.get("extra");
        if (extraNode != null && extraNode.isObject()) {
            ExtraCasesApiResponse extra = new ExtraCasesApiResponse();
            extra.setCaseNos(getListFromNode(extraNode, "excaseno"));
            extra.setCaseYears(getListFromNode(extraNode, "excaseyr"));
            extra.setCaseTypes(getListFromNode(extraNode, "excasetype"));
            extra.setPetitionerNames(getListFromNode(extraNode, "expname"));
            extra.setRespondentNames(getListFromNode(extraNode, "exrname"));
            extra.setPetitionerAdvocateNames(getListFromNode(extraNode, "expadv"));
            extra.setRespondentAdvocateNames(getListFromNode(extraNode, "exradv"));
            response.setExtra(extra);
        }

        return response;
    }

    /**
     * Processes a single court case and its associated hearing
     *
     * @param apiResponse Parsed API response for the case
     * @param district District where the case is filed
     */
    private void processCourtCase(CourtCaseApiResponse apiResponse, String district) {
        // Process main case
        CourtCase mainCase = createOrUpdateMainCase(apiResponse, district);
        createOrUpdateHearing(apiResponse, mainCase);

        // Process any associated extra cases
        if (apiResponse.getExtra() != null && !apiResponse.getExtra().isEmpty()) {
            processExtraCases(apiResponse.getExtra(), mainCase, district);
        }
    }

    /**
     * Creates or updates the main court case in the database
     *
     * @param apiResponse Parsed API response data
     * @param district Court district
     * @return Saved court case entity
     */
    private CourtCase createOrUpdateMainCase(CourtCaseApiResponse apiResponse, String district) {
        String caseId = generateCaseId(apiResponse.getCaseNo(), apiResponse.getCaseYear(), district);

        CourtCase courtCase = courtCaseRepository.findById(caseId)
                .orElseGet(() -> {
                    CourtCase newCase = new CourtCase();
                    newCase.setCaseId(caseId);
                    newCase.setState("Tamil Nadu");
                    newCase.setDistrict(district);
                    newCase.setCourtComplex(district + " High Court");
                    newCase.setCourtLevel(CourtCase.CourtLevel.HIGH_COURT);
                    return newCase;
                });

        // Update case details with latest data
        String normalizedCaseType = caseTypeNormalizer.normalizeCaseType(apiResponse.getCaseType());
        courtCase.setCaseType(normalizedCaseType);
        courtCase.setCaseNo(apiResponse.getCaseNo());
        courtCase.setCaseYear(Integer.parseInt(apiResponse.getCaseYear()));
        courtCase.setPetitionerNames(apiResponse.getPetitionerNames());
        courtCase.setRespondentNames(apiResponse.getRespondentNames());
        courtCase.setPetitionerAdvocateNames(apiResponse.getPetitionerAdvocateNames());
        courtCase.setRespondentAdvocateNames(apiResponse.getRespondentAdvocateNames());

        return courtCaseRepository.save(courtCase);
    }

    /**
     * Creates or updates hearing information for a court case
     *
     * @param apiResponse Parsed API response data
     * @param courtCase Associated court case entity
     */
    private void createOrUpdateHearing(CourtCaseApiResponse apiResponse, CourtCase courtCase) {
        String hearingId = generateHearingId(courtCase.getCaseId(), apiResponse.getCourtRemarks());

        CourtHearing hearing = courtHearingRepository.findByHearingId(hearingId)
                .orElseGet(CourtHearing::new);

        hearing.setHearingId(hearingId);
        hearing.setCourtCase(courtCase);
        hearing.setCourtNo(apiResponse.getCourtNo());
        hearing.setStage(apiResponse.getStageName());
        hearing.setHearingDatetime(parseHearingDateTime(apiResponse.getCourtRemarks()));
        hearing.setCourtRemarks(apiResponse.getCourtRemarks());

        courtHearingRepository.save(hearing);
    }

    /**
     * Processes extra cases associated with the main case
     *
     * @param extraCases Extra cases data from API response
     * @param mainCase Parent main case entity
     * @param district Court district
     */
    private void processExtraCases(ExtraCasesApiResponse extraCases, CourtCase mainCase, String district) {
        if (extraCases.getCaseNos().isEmpty()) return;

        for (int i = 0; i < extraCases.getCaseNos().size(); i++) {
            try {
                String caseNo = getAtIndex(extraCases.getCaseNos(), i);
                String caseYear = getAtIndex(extraCases.getCaseYears(), i);

                if (caseNo == null || caseNo.isEmpty() || caseYear == null || caseYear.isEmpty()) {
                    continue;
                }

                String caseId = generateCaseId(caseNo, caseYear, district);
                CourtCase extraCase = courtCaseRepository.findById(caseId)
                        .orElseGet(() -> {
                            CourtCase newCase = new CourtCase();
                            newCase.setCaseId(caseId);
                            newCase.setState("Tamil Nadu");
                            newCase.setDistrict(district);
                            newCase.setCourtComplex(district + " High Court");
                            newCase.setCourtLevel(CourtCase.CourtLevel.HIGH_COURT);
                            newCase.setParentCase(mainCase);
                            return newCase;
                        });

                // Update extra case details

                String extraCaseTypeRaw = getAtIndex(extraCases.getCaseTypes(), i);
                String normalizedExtraCaseType = caseTypeNormalizer.normalizeCaseType(extraCaseTypeRaw);
                extraCase.setCaseType(normalizedExtraCaseType);
                extraCase.setCaseNo(caseNo);
                extraCase.setCaseYear(Integer.parseInt(caseYear));
                extraCase.setPetitionerNames(nameDataCleaner.cleanNames(getAtIndex(extraCases.getPetitionerNames(), i)));
                extraCase.setRespondentNames(nameDataCleaner.cleanNames(getAtIndex(extraCases.getRespondentNames(), i)));
                extraCase.setPetitionerAdvocateNames(nameDataCleaner.cleanNames(getAtIndex(extraCases.getPetitionerAdvocateNames(), i)));
                extraCase.setRespondentAdvocateNames(nameDataCleaner.cleanNames(getAtIndex(extraCases.getRespondentAdvocateNames(), i)));

                courtCaseRepository.save(extraCase);

            } catch (Exception e) {
                logger.error("Failed to process extra case at index {}: {}", i, e.getMessage());
            }
        }
    }

    // Helper methods for JSON parsing

    /**
     * Extracts text value from JSON node
     */
    private String getText(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && fieldNode.isTextual()) ? fieldNode.asText().trim() : "";
    }

    /**
     * Extracts text value or first element from array field
     */
    private String getTextOrFirstFromArray(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null) return "";
        if (fieldNode.isTextual()) return fieldNode.asText().trim();
        if (fieldNode.isArray() && fieldNode.size() > 0) return fieldNode.get(0).asText().trim();
        return "";
    }

    /**
     * Extracts list of strings from JSON node
     */
    private List<String> getListFromNode(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        List<String> result = new ArrayList<>();

        if (fieldNode != null) {
            if (fieldNode.isTextual()) {
                result.add(fieldNode.asText().trim());
            } else if (fieldNode.isArray()) {
                for (JsonNode item : fieldNode) {
                    if (item.isTextual()) {
                        result.add(item.asText().trim());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Safely gets element from list by index
     */
    private String getAtIndex(List<String> list, int index) {
        return (list != null && index < list.size()) ? list.get(index) : "";
    }

    /**
     * Generates unique case ID based on case details
     */
    private String generateCaseId(String caseNo, String caseYear, String district) {
        return "TN-HC-" + district + "-" + caseNo + "-" + caseYear;
    }

    /**
     * Generates unique hearing ID based on case and remarks
     */
    private String generateHearingId(String caseId, String courtRemarks) {
        return caseId + "-" + Math.abs(courtRemarks.hashCode());
    }

    /**
     * Parses hearing date and time from court remarks text
     * Falls back to default time if parsing fails
     */
    private LocalDateTime parseHearingDateTime(String courtRemarks) {
        try {
            Matcher matcher = DATE_PATTERN.matcher(courtRemarks);
            if (matcher.find()) {
                String day = matcher.group(1);
                String monthName = matcher.group(2);
                String year = matcher.group(3);
                String hour = matcher.group(4);
                String minute = matcher.group(5);
                String period = matcher.group(6);

                int month = MONTH_MAP.getOrDefault(monthName.toUpperCase(), 1);
                int hourInt = parseHour(hour, period);

                return LocalDateTime.of(
                        Integer.parseInt(year),
                        month,
                        Integer.parseInt(day),
                        hourInt,
                        Integer.parseInt(minute)
                );
            }
        } catch (Exception e) {
            logger.warn("Failed to parse hearing date from remarks: {}", courtRemarks);
        }

        // Default to 10:00 AM if parsing fails
        return LocalDate.now().plusDays(1).atTime(10, 0);
    }

    /**
     * Converts 12-hour format to 24-hour format
     */
    private int parseHour(String hour, String period) {
        int hourInt = Integer.parseInt(hour);
        if ("P.M.".equals(period) && hourInt < 12) {
            hourInt += 12;
        } else if ("A.M.".equals(period) && hourInt == 12) {
            hourInt = 0;
        }
        return hourInt;
    }

    /**
     * Creates month name to number mapping
     */
    private static Map<String, Integer> createMonthMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("JANUARY", 1); map.put("FEBRUARY", 2); map.put("MARCH", 3);
        map.put("APRIL", 4); map.put("MAY", 5); map.put("JUNE", 6);
        map.put("JULY", 7); map.put("AUGUST", 8); map.put("SEPTEMBER", 9);
        map.put("OCTOBER", 10); map.put("NOVEMBER", 11); map.put("DECEMBER", 12);
        return map;
    }

    // API Response Data Classes

    /**
     * Represents the main court case data from API response
     */
    @Getter
    @Setter
    public static class CourtCaseApiResponse {
        private String courtNo;
        private String courtRemarks;
        private String stageName;
        private String caseType;
        private String caseNo;
        private String caseYear;
        private String petitionerNames;
        private String respondentNames;
        private String petitionerAdvocateNames;
        private String respondentAdvocateNames;
        private ExtraCasesApiResponse extra;
    }

    /**
     * Represents additional cases associated with the main case
     */
    @Getter
    @Setter
    public static class ExtraCasesApiResponse {
        private List<String> caseTypes = new ArrayList<>();
        private List<String> caseNos = new ArrayList<>();
        private List<String> caseYears = new ArrayList<>();
        private List<String> petitionerNames = new ArrayList<>();
        private List<String> respondentNames = new ArrayList<>();
        private List<String> petitionerAdvocateNames = new ArrayList<>();
        private List<String> respondentAdvocateNames = new ArrayList<>();

        public boolean isEmpty() {
            return caseNos.isEmpty() && caseYears.isEmpty();
        }
    }

    /**
     * Tracks import results for reporting
     */
    private static class ImportResult {
        private int successfulImports = 0;
        private int failedImports = 0;

        public void incrementSuccess() { successfulImports++; }
        public void incrementFailed() { failedImports++; }
        public void add(ImportResult other) {
            this.successfulImports += other.successfulImports;
            this.failedImports += other.failedImports;
        }

        @Override
        public String toString() {
            return String.format("Successful: %d, Failed: %d", successfulImports, failedImports);
        }
    }
}