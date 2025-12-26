package com.CourtAssist.service.user;

import com.CourtAssist.model.FcmToken;
import com.CourtAssist.model.Users;
import com.CourtAssist.repository.FcmTokenRepository;
import com.CourtAssist.repository.UsersRepository;
import com.CourtAssist.util.ContactUtils;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.CourtAssist.dto.payload.UserProfileRequest;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UsersRepository usersRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FcmTokenRepository fcmTokenRepository;

    public UserService(UsersRepository usersRepository, BCryptPasswordEncoder passwordEncoder, FcmTokenRepository fcmTokenRepository) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.fcmTokenRepository = fcmTokenRepository;
    }

    /**
     * Register a new user with validation and duplicate checking
     */
    public Users register(Users user) {
        // Validate input
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }

        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }

        // Validate that at least one contact method is provided
        if (!StringUtils.hasText(user.getEmail()) && !StringUtils.hasText(user.getMobileNo())) {
            throw new IllegalArgumentException("Either email or mobile number must be provided");
        }

        // Set default role if not provided
        if (user.getRole() == null) {
            user.setRole(Users.UserRole.USER);
        }

        // Check for duplicate username
        if (usersRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check for duplicate email (if provided)
        if (StringUtils.hasText(user.getEmail()) && usersRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check for duplicate mobile number (if provided)
        if (StringUtils.hasText(user.getMobileNo()) && usersRepository.existsByMobileNo(user.getMobileNo())) {
            throw new IllegalArgumentException("Mobile number already registered");
        }

        // For advocates, validate advocate name
        if (user.getRole() == Users.UserRole.ADVOCATE && !StringUtils.hasText(user.getAdvocateName())) {
            throw new IllegalArgumentException("Advocate name is required for advocate role");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save user
        return usersRepository.save(user);
    }

    public Users getUserProfile(String username) {
        System.out.println("user service method called");
        return usersRepository.findByUsername(username)
                .orElseGet(() -> usersRepository.findByEmail(username)
                        .orElseGet(() -> usersRepository.findByMobileNo(username)
                                .orElseThrow(() -> new RuntimeException("User not found"))));
    }

    public Users updateUserProfile(String currentUsername,UserProfileRequest request) {
        Users user = usersRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate that either email or mobileNo is provided
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getMobileNo())) {
            throw new RuntimeException("Either email or mobile number must be provided");
        }

        // Check if new username is already taken (if changing username)
        if (StringUtils.hasText(request.getUsername()) &&
                !request.getUsername().equals(currentUsername) &&
                usersRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // Check if email is already taken (if changing email)
        if (StringUtils.hasText(request.getEmail()) &&
                !request.getEmail().equals(user.getEmail()) &&
                usersRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Check if mobile number is already taken (if changing mobile number)
        if (StringUtils.hasText(request.getMobileNo()) &&
                !request.getMobileNo().equals(user.getMobileNo()) &&
                usersRepository.existsByMobileNo(request.getMobileNo())) {
            throw new RuntimeException("Mobile number already registered");
        }

        // Update fields if provided
        if (StringUtils.hasText(request.getUsername())) {
            user.setUsername(request.getUsername());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getMobileNo())) {
            user.setMobileNo(request.getMobileNo());
        }

        // FIX: Update role if provided
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                Users.UserRole newRole = Users.UserRole.valueOf(request.getRole().toUpperCase());
                user.setRole(newRole);

                // If changing to ADVOCATE, validate advocate name
                if (newRole == Users.UserRole.ADVOCATE) {
                    if (!StringUtils.hasText(request.getAdvocateName())) {
                        throw new RuntimeException("Advocate name is required when selecting advocate role");
                    }
                    user.setAdvocateName(request.getAdvocateName());
                } else {
                    // If changing from ADVOCATE to USER, clear advocate name
                    user.setAdvocateName(null);
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role: " + request.getRole());
            }
        } else if (user.getRole() == Users.UserRole.ADVOCATE &&
                StringUtils.hasText(request.getAdvocateName())) {
            // Update advocate name for existing advocates
            user.setAdvocateName(request.getAdvocateName());
        }

        return usersRepository.save(user);
    }

    public void updatePassword(String username, String currentPassword, String newPassword) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters long");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

    }
    /**
     * Reset password without requiring current password (for forgot password flow)
     */
    public void resetPasswordWithoutCurrent(String username, String newPassword) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate new password
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long");
        }

        // Update password without checking current password
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

    }

    public Users findByContact(String contact) {
        // Normalize contact
        String normalizedContact = ContactUtils.normalizeContact(contact);

        // Try to find by email or mobile
        if (normalizedContact.contains("@")) {
            return usersRepository.findByEmail(normalizedContact)
                    .orElse(null);
        } else {
            return usersRepository.findByMobileNo(normalizedContact)
                    .orElse(null);
        }
    }

    public void deleteAccount(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        usersRepository.delete(user);
    }

    /**
     * Check if a user exists by contact (email or mobile)
     */
    public boolean userExists(String contact) {
        contact = ContactUtils.normalizeContact(contact);
        return usersRepository.findByContact(contact) != null;
    }

    @Transactional
    public void saveOrUpdateFcmToken(Long userId, String newToken) {
        // Check if a token already exists for this user
        FcmToken existingToken = fcmTokenRepository.findByUserId(userId);

        if (existingToken != null) {
            // Update existing token
            existingToken.setFcmToken(newToken);
            existingToken.setLastUpdated(LocalDateTime.now());
        } else {
            // Create a new token entity
            FcmToken newFcmToken = new FcmToken();
            newFcmToken.setUserId(userId);
            newFcmToken.setFcmToken(newToken);
            newFcmToken.setLastUpdated(LocalDateTime.now());
            fcmTokenRepository.save(newFcmToken);
        }
    }
}