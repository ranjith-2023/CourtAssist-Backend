package com.CourtAssist.controller;

import com.CourtAssist.dto.payload.LoginRequest;
import com.CourtAssist.service.jwt.JwtService;
import com.CourtAssist.service.user.UserDetailsServiceImpl;
import com.CourtAssist.service.user.UserPrincipal;
import com.CourtAssist.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          UserDetailsServiceImpl userDetailsService,
                          UserService userService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // FIX: Get actual username from UserPrincipal
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String username = userPrincipal.getUsername();

            String accessToken = jwtService.generateAccessToken(username);
            String refreshToken = jwtService.generateRefreshToken(username);

            setRefreshTokenCookie(response, refreshToken);

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "tokenType", "Bearer",
                    "expiresIn", jwtService.getAccessTokenExpiration()
            ));
        } catch (AuthenticationException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token missing"));
        }

        try {
            String username = jwtService.extractUsername(refreshToken);
            var userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.validateToken(refreshToken, userDetails)) {
                String newAccessToken = jwtService.generateAccessToken(username);
                String newRefreshToken = jwtService.generateRefreshToken(username);
                setRefreshTokenCookie(response, newRefreshToken);

                return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid refresh token"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
    }

    // Helper methods
    private String extractRefreshToken(HttpServletRequest request) {
        // Extract from cookie or Authorization header
        return null; // Implementation depends on your token storage strategy
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        // Set HTTP-only secure cookie
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        // Clear the refresh token cookie
    }
}