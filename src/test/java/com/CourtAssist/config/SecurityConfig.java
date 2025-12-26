package com.CourtAssist.config;

import com.CourtAssist.filter.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;

    public SecurityConfig(UserDetailsService userDetailsService, JwtFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Configure allowed origins (you can use profiles for different environments)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",          // Development
                "http://localhost:5173",          // Alternative dev port
                "https://courtassist.com",        // Production domain
                "https://www.courtassist.com"     // Production www domain
        ));

        // Configure allowed methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // Configure allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-XSRF-TOKEN",
                "Cache-Control",
                "X-Forwarded-For",
                "X-Forwarded-Proto"
        ));

        // Configure exposed headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-XSRF-TOKEN",
                "New-Access-Token",
                "Access-Control-Allow-Headers",
                "Access-Control-Allow-Origin"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour cache for preflight responses

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        provider.setHideUserNotFoundExceptions(false); // Better error messages
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        // Explicitly set your DaoAuthenticationProvider
        authenticationManagerBuilder.authenticationProvider(authenticationProvider());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CORS Configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF Configuration (disabled for stateless JWT API)
                .csrf(csrf -> csrf.disable())

                // Session Management (stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixation().migrateSession()
                )

                // Exception Handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Authentication required\", \"code\": \"UNAUTHORIZED\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Access denied\", \"code\": \"FORBIDDEN\"}"
                            );
                        })
                )

                // Authorization Rules
                .authorizeHttpRequests(authz -> authz
                        // Preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Allow WebSocket connections
                        .requestMatchers("/ws/**").permitAll()
                        // Static resources
                        .requestMatchers(
                                "/",
                                "/favicon.ico",
                                "/error",
                                "/actuator/health",
                                "/actuator/info",
                                "/test/**"
                        ).permitAll()

                        // Public API endpoints
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()

                        // User registration and verification
                        .requestMatchers(
                                "/api/user/register",
                                "/api/user/send-contact-verification-otp",
                                "/api/user/verify-contact-verification-otp",
                                "/api/user/send-password-reset-otp",
                                "/api/user/verify-password-reset-otp",
                                "/api/user/reset-password"
                        ).permitAll()

                        // Public data endpoints (read-only)
                        .requestMatchers(HttpMethod.GET,
                                "/api/courts/**",
                                "/api/cases/public/**",
                                "/api/advocates/public/**"
                        ).permitAll()

                        // User endpoints (authenticated users)
                        .requestMatchers("/api/user/**").authenticated()

                        // Advocate endpoints
                        .requestMatchers("/api/advocate/**").hasAnyRole("ADVOCATE", "ADMIN")

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Actuator endpoints (for monitoring)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Default rule - all other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Security Headers
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self';")
                        )
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                )

                // Authentication Provider
                .authenticationProvider(authenticationProvider())

                // JWT Filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Build configuration
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strong encryption
    }

    // Additional security beans for production
    @Bean
    public SecurityLogger securityLogger() {
        return new SecurityLogger();
    }

    // Security event logger
    public static class SecurityLogger {
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecurityLogger.class);

        public void logSecurityEvent(String event, String details) {
            logger.info("Security Event: {} - {}", event, details);
        }
    }
}

