package com.CourtAssist.service.subscription;

import com.CourtAssist.model.Users;
import com.CourtAssist.model.UserSubscription;
import com.CourtAssist.model.CourtCase;
import com.CourtAssist.repository.UsersRepository;
import com.CourtAssist.repository.UserSubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserSubscriptionService {

    @Autowired
    private UserSubscriptionRepository subscriptionRepository;

    @Autowired
    private UsersRepository userRepository;

    // Advocate subscription
    public UserSubscription createAdvocateSubscription(Long userId, CourtCase.CourtLevel courtLevel,
                                                       String state, String district, String courtComplex,
                                                       String courtName, String caseNo) {

        if(userId == null){
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Users user = getUserById(userId);

        if (user.getRole() != Users.UserRole.ADVOCATE) {
            throw new IllegalArgumentException("Only users with ADVOCATE role can create advocate subscriptions");
        }

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setCourtLevel(courtLevel);
        subscription.setState(state);
        subscription.setDistrict(district);
        subscription.setCourtComplex(courtComplex);
        subscription.setCourtName(courtName);
        subscription.setCaseNo(caseNo);
        subscription.setAdvocateName(user.getAdvocateName());

        return subscriptionRepository.save(subscription);
    }

    // Litigant subscription
    public UserSubscription createLitigantSubscription(Long userId, CourtCase.CourtLevel courtLevel,
                                                       String state, String district, String courtComplex,
                                                       String courtName, String litigantName, String caseNo) {
        Users user = getUserById(userId);

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setCourtLevel(courtLevel);
        subscription.setState(state);
        subscription.setDistrict(district);
        subscription.setCourtComplex(courtComplex);
        subscription.setCourtName(courtName);
        subscription.setLitigantName(litigantName);
        subscription.setCaseNo(caseNo);

        return subscriptionRepository.save(subscription);
    }

    // Case details subscription
    public UserSubscription createCaseDetailsSubscription(Long userId, CourtCase.CourtLevel courtLevel,
                                                          String state, String district, String courtComplex,
                                                          String courtName, String caseType, String caseNo,
                                                          Integer caseYear) {
        Users user = getUserById(userId);

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setCourtLevel(courtLevel);
        subscription.setState(state);
        subscription.setDistrict(district);
        subscription.setCourtComplex(courtComplex);
        subscription.setCourtName(courtName);
        subscription.setCaseType(caseType);
        subscription.setCaseNo(caseNo);
        subscription.setCaseYear(caseYear);

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public List<UserSubscription> updateAdvocateNameInSubscriptions(Long userId, String newAdvocateName) {
        Users user = getUserById(userId);

        if (user.getRole() != Users.UserRole.ADVOCATE) {
            throw new IllegalArgumentException("Only users with ADVOCATE role can update advocate subscriptions");
        }

        // Get all subscriptions for this user that have advocate name set
        List<UserSubscription> advocateSubscriptions = subscriptionRepository.findByUserId(userId).stream()
                .filter(subscription -> subscription.getAdvocateName() != null && !subscription.getAdvocateName().trim().isEmpty())
                .toList();

        // Update advocate name in all subscriptions
        for (UserSubscription subscription : advocateSubscriptions) {
            subscription.setAdvocateName(newAdvocateName);
        }

        return subscriptionRepository.saveAll(advocateSubscriptions);
    }

    public List<UserSubscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public void deleteSubscription(Long subscriptionId) {
        if (!subscriptionRepository.existsById(subscriptionId)) {
            throw new IllegalArgumentException("Subscription not found with ID: " + subscriptionId);
        }
        subscriptionRepository.deleteById(subscriptionId);
    }

    public UserSubscription getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));
    }

    // Helper method to get user by ID
    private Users getUserById(Long userId) {
        Optional<Users> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        return userOptional.get();
    }
}