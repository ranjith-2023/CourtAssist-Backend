package com.CourtAssist.controller;

import com.CourtAssist.dto.payload.FcmTokenUpdateRequest;
import com.CourtAssist.model.Users;
import com.CourtAssist.dto.payload.PasswordUpdateRequest;
import com.CourtAssist.dto.payload.UserProfileRequest;
import com.CourtAssist.dto.payload.UserRegistrationRequest;
import com.CourtAssist.repository.UsersRepository;
import com.CourtAssist.service.jwt.JwtService;
import com.CourtAssist.service.user.UserPrincipal;
import com.CourtAssist.service.user.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/user")
public class UserController {

    private final JwtService jwtService;
    private final UserService userService;
    private final UsersRepository usersRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(JwtService jwtService, UserService userService, UsersRepository usersRepository) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.usersRepository = usersRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            Users user = new Users();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());
            user.setMobileNo(request.getMobileNo());

            // Handle role assignment
            if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
                try {
                    Users.UserRole role = Users.UserRole.valueOf(request.getRole().toUpperCase());
                    user.setRole(role);

                    // Validate advocate name for advocate role
                    if (role == Users.UserRole.ADVOCATE) {
                        if (request.getAdvocateName() == null || request.getAdvocateName().trim().isEmpty()) {
                            return ResponseEntity.badRequest().body(Map.of("error", "Advocate name is required for advocate role"));
                        }
                        user.setAdvocateName(request.getAdvocateName().trim());
                    }
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + request.getRole()));
                }
            } else {
                // Default to USER if no role specified
                user.setRole(Users.UserRole.USER);
            }

            Users registeredUser = userService.register(user);
            String accessToken = jwtService.generateAccessToken(registeredUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("user", registeredUser);
            response.put("message", "Registration successful");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        try {
            System.out.println("user controller method called");
            String username = authentication.getName();
            Users user = userService.getUserProfile(username);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(Authentication authentication,
                                               @Valid @RequestBody UserProfileRequest request) {
        try {
            String username = authentication.getName();
            Users updatedUser = userService.updateUserProfile(username, request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(Authentication authentication,
                                            @Valid @RequestBody PasswordUpdateRequest request) {
        try {
            String username = authentication.getName();
            userService.updatePassword(username, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok().body(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String contact = request.get("contact");
            String newPassword = request.get("newPassword");

            if (contact == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Contact and new password are required"));
            }

            Users user = userService.findByContact(contact);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            // FIX: Use a dedicated reset password method that doesn't require current password
            userService.resetPasswordWithoutCurrent(user.getUsername(), newPassword);

            return ResponseEntity.ok().body(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(Authentication authentication) {
        try {
            String username = authentication.getName();
            userService.deleteAccount(username);
            return ResponseEntity.ok().body(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@RequestBody FcmTokenUpdateRequest request,@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // Inject UserPrincipal
        if (userPrincipal == null) {
            logger.warn("Received FCM token update request from an unauthenticated source.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Get the Users entity from UserPrincipal
        Users user = userPrincipal.getUser();
        userService.saveOrUpdateFcmToken(user.getUserId(), request.getFcmToken());
        return ResponseEntity.noContent().build();
    }

}