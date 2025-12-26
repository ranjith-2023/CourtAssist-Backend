// NotificationProcessingService.java
package com.CourtAssist.service.notification;

import com.CourtAssist.dto.NotificationMessageDTO;
import com.CourtAssist.model.*;
import com.CourtAssist.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationProcessingService.class);

    private final CourtHearingRepository hearingRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final CourtCaseRepository caseRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationHelperService notificationHelperService;
    private final NameMatchingService nameMatchingService;
    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRespository;

    /**
     * Constructor for dependency injection
     */
    public NotificationProcessingService(CourtHearingRepository hearingRepository,
                                         UserSubscriptionRepository subscriptionRepository,
                                         CourtCaseRepository caseRepository,
                                         NotificationRepository notificationRepository,
                                         NotificationHelperService notificationHelperService,
                                         NameMatchingService nameMatchingService, FcmService fcmService, FcmTokenRepository fcmTokenRespository) {
        this.hearingRepository = hearingRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.caseRepository = caseRepository;
        this.notificationRepository = notificationRepository;
        this.notificationHelperService = notificationHelperService;
        this.nameMatchingService = nameMatchingService;
        this.fcmService = fcmService;
        this.fcmTokenRespository = fcmTokenRespository;
    }

    /**
     * Main processing method for upcoming hearing notifications
     * Processes all hearings for a given date and sends notifications for matching subscriptions
     *
     * @param date The date for which to process hearings
     */
    @Transactional
    public void processUpcomingHearingNotificationsForDate(LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(23, 59, 59);

        logger.info("Starting notification processing for date: {}", date);

        List<CourtHearing> upcomingHearings = hearingRepository.findByHearingDatetimeBetween(startTime, endTime);
        logger.info("Found {} hearings on {}", upcomingHearings.size(), date);

        if (upcomingHearings.isEmpty()) {
            logger.info("No hearings found for date {}", date);
            return;
        }

        List<UserSubscription> allSubscriptions = subscriptionRepository.findAll();
        logger.info("Total subscriptions in system: {}", allSubscriptions.size());

        int totalNotificationsSent = 0;

        for (CourtHearing hearing : upcomingHearings) {
            totalNotificationsSent += processSingleHearing(hearing, allSubscriptions);
        }

        logger.info("Processing complete: Sent {} total notifications for date {}", totalNotificationsSent, date);
    }

    /**
     * Processes a single hearing and sends notifications to matching subscribers
     *
     * @param hearing The hearing to process
     * @param allSubscriptions List of all user subscriptions
     * @return Number of notifications sent for this hearing
     */
    private int processSingleHearing(CourtHearing hearing, List<UserSubscription> allSubscriptions) {
        CourtCase mainCase = hearing.getCourtCase();
        List<CourtCase> allRelatedCases = new ArrayList<>();
        allRelatedCases.add(mainCase);
        allRelatedCases.addAll(caseRepository.findByParentCase(mainCase));

        int notificationsForThisHearing = 0;
        Set<Long> usersNotifiedInThisHearing = new HashSet<>();

        for (CourtCase courtCase : allRelatedCases) {
            List<UserSubscription> matchingSubscriptions = findMatchingSubscriptions(courtCase, allSubscriptions);

            for (UserSubscription subscription : matchingSubscriptions) {
                // Enhanced null checking
                if (subscription.getUser() == null) {
                    logger.warn("Skipping subscription with null user for case: {}", courtCase.getCaseId());
                    continue;
                }

                Long userId = subscription.getUser().getUserId();
                if (userId == null || usersNotifiedInThisHearing.contains(userId)) {
                    continue;
                }

                try {
                    NotificationMessageDTO notificationDto = NotificationMessageDTO.createFrom(courtCase, hearing, subscription);

                    // Send push notifications with null safety
                    List<FcmToken> userFcmTokens = fcmTokenRespository.getTokensByUserId(userId);
                    if (userFcmTokens != null && !userFcmTokens.isEmpty()) {
                        for(FcmToken userFcmToken : userFcmTokens) {
                            if (userFcmToken != null && userFcmToken.getFcmToken() != null) {
                                fcmService.sendNotificationToToken(
                                        userFcmToken.getFcmToken(),
                                        "Court Hearing Alert",
                                        notificationDto.getFormattedMessage()
                                );
                            }
                        }
                    }

                    // Send email/SMS notifications with null safety
                    String email = subscription.getUser().getEmail();
                    String mobileNo = subscription.getUser().getMobileNo();

                    if (email != null || mobileNo != null) {
                        notificationHelperService.sendHearingNotification(email, mobileNo, notificationDto);
                    }

                    saveNotificationToDatabase(subscription.getUser(), hearing, courtCase, notificationDto);
                    notificationsForThisHearing++;
                    usersNotifiedInThisHearing.add(userId);

                    logger.debug("Notification sent for CaseNo '{}', User '{}'",
                            courtCase.getCaseNo(), userId);

                } catch (Exception e) {
                    logger.error("Failed to notify User {} for Case {}: {}",
                            userId, courtCase.getCaseId(), e.getMessage());
                }
            }
        }
        return notificationsForThisHearing;
    }

    /**
     * Finds subscriptions that match the court case using AND logic
     * Both case number AND advocate name must match (if specified)
     *
     * @param courtCase The court case to match against
     * @param allSubscriptions List of all subscriptions to check
     * @return List of matching subscriptions
     */
    private List<UserSubscription> findMatchingSubscriptions(CourtCase courtCase, List<UserSubscription> allSubscriptions) {
        return allSubscriptions.stream()
                .filter(subscription -> matchesSubscriptionCriteria(subscription, courtCase))
                .collect(Collectors.toList());
    }

    /**
     * Determines if a subscription matches all specified court case criteria
     * Uses exact matching for required fields and flexible matching for case numbers
     *
     * @param subscription User subscription to check
     * @param courtCase Court case to match against
     * @return true if all specified criteria match
     */
    private boolean matchesSubscriptionCriteria(UserSubscription subscription, CourtCase courtCase) {
        // Case number matching (flexible partial matching)
        if (StringUtils.hasText(subscription.getCaseNo())) {
            boolean caseNoMatch = isFlexibleCaseNumberMatch(subscription.getCaseNo(), courtCase.getCaseNo(), courtCase.getCaseId());
            if (!caseNoMatch) {
                return false;
            }
        }

        // Advocate name matching (flexible partial matching)
        if (StringUtils.hasText(subscription.getAdvocateName())) {
            boolean advocateMatch = nameMatchingService.matchesAnyName(
                    subscription.getAdvocateName(),
                    courtCase.getPetitionerAdvocateNames(),
                    courtCase.getRespondentAdvocateNames()
            );
            if (!advocateMatch) {
                return false;
            }
        }

        // Case year matching (exact matching)
        if (subscription.getCaseYear() != null && !subscription.getCaseYear().equals(courtCase.getCaseYear())) {
            return false;
        }

        // Court level matching (exact matching)
        if (subscription.getCourtLevel() != null && subscription.getCourtLevel() != courtCase.getCourtLevel()) {
            return false;
        }

        // All specified criteria matched
        return true;
    }

    /**
     * Performs flexible case number matching to handle partial matches and different formats
     * Examples: '26954' matches 'TN-HC-Madurai-26954-2025', '26954/2025', etc.
     *
     * @param subscriptionCaseNo Case number from subscription
     * @param courtCaseNo Case number from court record
     * @param courtCaseId Case ID from court record
     * @return true if a flexible match is found
     */
    private boolean isFlexibleCaseNumberMatch(String subscriptionCaseNo, String courtCaseNo, String courtCaseId) {
        if (!StringUtils.hasText(subscriptionCaseNo)) return false;

        // Normalize: remove spaces, special characters, convert to lowercase
        String normalizedSubscriptionCaseNo = subscriptionCaseNo.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        // Check against case number
        if (StringUtils.hasText(courtCaseNo)) {
            String normalizedCourtCaseNo = courtCaseNo.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (normalizedCourtCaseNo.contains(normalizedSubscriptionCaseNo)) {
                return true;
            }
        }

        // Check against case ID (important for generated case IDs like 'TN-HC-Madurai-26954-2025')
        if (StringUtils.hasText(courtCaseId)) {
            String normalizedCaseId = courtCaseId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (normalizedCaseId.contains(normalizedSubscriptionCaseNo)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Saves notification record to database for audit and tracking purposes
     *
     * @param user The user who received the notification
     * @param hearing The hearing that triggered the notification
     * @param courtCase The court case associated with the hearing
     * @param dto The notification message data
     */
    private void saveNotificationToDatabase(Users user, CourtHearing hearing, CourtCase courtCase, NotificationMessageDTO dto) {
        try {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setCourtCase(courtCase);
            notification.setHearingId(hearing.getHearingId());
            notification.setCaseRef(dto.getCaseRef());
            notification.setHearingDate(dto.getHearingDateTime().toLocalDate());
            notification.setHearingTime(dto.getHearingDateTime().toLocalTime());
            notification.setCourt(dto.getCourt());
            notification.setStage(dto.getStage());
            notification.setParties(dto.getParties());
            notification.setAdvocates(dto.getAdvocates());
            notification.setIsRead(false);
            notification.setIsSent(true);

            notificationRepository.save(notification);
            logger.debug("Notification saved to database for user {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Error saving notification for user {}: {}", user.getUserId(), e.getMessage());
        }
    }
}