package com.CourtAssist.service.scheduler;

import com.CourtAssist.service.cleanup.DataCleanUpService;
import com.CourtAssist.service.courtdata.CourtDataImportService;
import com.CourtAssist.service.notification.NotificationProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CourtDataScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CourtDataScheduler.class);

    private final CourtDataImportService courtDataImportService;
    private final NotificationProcessingService notificationProcessingService;
    private LocalDate lastProcessedDate = null;
    private final DataCleanUpService cleanUpService;

    public CourtDataScheduler(CourtDataImportService courtDataImportService,
                              NotificationProcessingService notificationProcessingService, DataCleanUpService cleanUpService) {
        this.courtDataImportService = courtDataImportService;
        this.notificationProcessingService = notificationProcessingService;
        this.cleanUpService = cleanUpService;
    }

    /**
     * Scheduled execution - runs every 1 hour starting from 12:00 AM daily
     * Checks if we haven't processed today's data yet
     */
    @Scheduled(cron = "${court-data.scheduler.cron:0 0 0/1 * * ?}")
    public void scheduledExecution() {
        logger.info("Starting scheduled court data import check");
        executeScheduledTask();
    }

    /**
     * Task Execution after Application Started
     */
    /**  @EventListener(ApplicationReadyEvent.class)
    *    public void onApplicationReady(){
    *        logger.info("Task triggered due to application start");
    *        executeScheduledTask();
    *    }
    */

    /**
     * Core method that orchestrates data import and notification processing
     * Only imports data once per day
     */
    @Transactional
    public void executeScheduledTask() {
        try {
            LocalDate today = LocalDate.now();

            // Check if we've already processed data today
            if (lastProcessedDate != null && lastProcessedDate.equals(today)) {
                logger.info("Data already processed for today ({}). Skipping import until next day.", today);
                return;
            }

            LocalDate targetDate = today.plusDays(1); // Process tomorrow's data

            logger.info("Starting court data scheduler execution for date: {}", targetDate);
            long startTime = System.currentTimeMillis();

            // Step 1: Import court data
            logger.info("Step 1: Importing court data...");
            String importResult = courtDataImportService.importCourtDataForDate(targetDate);
            logger.info("Court data import completed: {}", importResult);

            // Step 2: Process notifications
            logger.info("Step 2: Processing notifications...");
            notificationProcessingService.processUpcomingHearingNotificationsForDate(targetDate);
            logger.info("Notification processing completed");

            // Mark today as processed
            lastProcessedDate = today;

            long endTime = System.currentTimeMillis();
            logger.info("Scheduler execution completed in {} ms. Next import will occur after 12:00 AM tomorrow.",
                    (endTime - startTime));

        } catch (Exception e) {
            logger.error("Error in scheduled task execution: {}", e.getMessage(), e);
            // You can add retry logic or alerting here
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // every midnight
    @Transactional
    public void cleanUpData(){
        cleanUpService.cleanUpData(LocalDateTime.now());
    }

}