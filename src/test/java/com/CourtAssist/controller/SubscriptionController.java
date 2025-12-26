package com.CourtAssist.controller;

import com.CourtAssist.model.CourtCase;
import com.CourtAssist.model.UserSubscription;
import com.CourtAssist.service.subscription.UserSubscriptionService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private UserSubscriptionService subscriptionService;

    // Advocate subscription
    @PostMapping("/advocate")
    public ResponseEntity<?> subscribeAdvocate(
            @RequestBody AdvocateSubscriptionRequest request) {
        try {
            System.out.println("advocate controller called..");
            System.out.println(request.getUserId() +" "+ request.getCourtLevel() +" "+ request.getState() +" "+
                    request.getDistrict() +" "+ request.getCourtComplex() +" "+ request.getCourtName() +" "+
                    request.getCaseNo());
            UserSubscription subscription = subscriptionService.createAdvocateSubscription(
                    request.getUserId(), request.getCourtLevel(), request.getState(),
                    request.getDistrict(), request.getCourtComplex(), request.getCourtName(),
                    request.getCaseNo());
            System.out.println("object created..");
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Litigant subscription
    @PostMapping("/litigant")
    public ResponseEntity<?> subscribeLitigant(
            @RequestBody LitigantSubscriptionRequest request) {
        try {
            UserSubscription subscription = subscriptionService.createLitigantSubscription(
                    request.getUserId(), request.getCourtLevel(), request.getState(),
                    request.getDistrict(), request.getCourtComplex(), request.getCourtName(),
                    request.getLitigantName(), request.getCaseNo());
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Case details subscription
    @PostMapping("/case-details")
    public ResponseEntity<?> subscribeCaseDetails(
            @RequestBody CaseDetailsSubscriptionRequest request) {
        try {
            UserSubscription subscription = subscriptionService.createCaseDetailsSubscription(
                    request.getUserId(), request.getCourtLevel(), request.getState(),
                    request.getDistrict(), request.getCourtComplex(), request.getCourtName(),
                    request.getCaseType(), request.getCaseNo(), request.getCaseYear());
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get user's subscriptions
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserSubscription>> getUserSubscriptions(@PathVariable Long userId) {
        List<UserSubscription> subscriptions = subscriptionService.getUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    // Delete subscription
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<?> deleteSubscription(@PathVariable Long subscriptionId) {
        try {
            subscriptionService.deleteSubscription(subscriptionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Request DTOs
    @Setter
    @Getter
    public static class AdvocateSubscriptionRequest {
        private Long userId;
        private CourtCase.CourtLevel courtLevel;
        private String state;
        private String district;
        private String courtComplex;
        private String courtName;
        private String caseNo;
    }

    @Setter
    @Getter
    public static class LitigantSubscriptionRequest {
        private Long userId;
        private CourtCase.CourtLevel courtLevel;
        private String state;
        private String district;
        private String courtComplex;
        private String courtName;
        private String litigantName;
        private String caseNo;
    }

    @Setter
    @Getter
    public static class CaseDetailsSubscriptionRequest {
        private Long userId;
        private CourtCase.CourtLevel courtLevel;
        private String state;
        private String district;
        private String courtComplex;
        private String courtName;
        private String caseType;
        private String caseNo;
        private Integer caseYear;
    }
}