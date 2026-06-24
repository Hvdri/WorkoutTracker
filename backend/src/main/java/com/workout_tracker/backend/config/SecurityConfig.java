package com.workout_tracker.backend.config;

import com.workout_tracker.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Comma-separated origins from app.cors.allowed-origins (overridable via CORS_ALLOWED_ORIGINS env var)
    @Value("${app.cors.allowed-origins}")
    private String corsAllowedOrigins;

    // Lets the test profile turn CSRF off so existing MockMvc tests don't need to
    // attach a token on every POST/PUT/DELETE. Dev and prod keep it enabled. The
    // application-test.yaml override sets this to false.
    @Value("${app.security.csrf-enabled:true}")
    private boolean csrfEnabled;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Exposes Spring's AuthenticationManager so AuthService can call authenticate().
    // Spring auto-configures it to use our UserDetailsServiceImpl + PasswordEncoder.
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Split comma-separated list so multiple origins work in production
        // trim() handles "url1, url2" (space after comma) so CORS checks don't silently fail
        config.setAllowedOrigins(Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // Explicit headers — don't allow everything (*) unnecessarily
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // No credentials (cookies) needed — auth is done via Authorization header with JWT

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF posture. JWT in the Authorization header isn't auto-sent by the
            // browser, so a classic CSRF attack against this API is not exploitable
            // — but the course spec asks for "CSRF protection activă" for maximum
            // Spring Security credit, so we enable it here as defense-in-depth.
            //
            // CookieCsrfTokenRepository.withHttpOnlyFalse() writes the token to an
            // XSRF-TOKEN cookie that the frontend's axios interceptor reads and
            // echoes back as X-XSRF-TOKEN on state-changing requests. /api/auth/**
            // is exempt because login/register can't carry a token yet; /internal/**
            // is exempt because it's called server-to-server (no browser involved).
            //
            // Test profile flips csrfEnabled=false via application-test.yaml so the
            // existing MockMvc suite keeps working without per-test csrf() builders.
            .csrf(csrf -> {
                if (csrfEnabled) {
                    csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                            "/api/auth/**",
                            "/internal/**",
                            "/h2-console/**",
                            "/actuator/**");
                } else {
                    csrf.disable();
                }
            })
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // /internal/** is for inter-service calls from social-service (and future
                // notification-service). In production these would be reachable only from the
                // internal Docker network; here we permit them unauthenticated. Do NOT expose
                // /internal/ routes via any public-facing API gateway.
                .requestMatchers("/internal/**").permitAll()
                // We deliberately mix path-based and method-based enforcement here:
                //   * Path-based (this block) opens specific public reads. New GETs stay private by default.
                //   * Method-based (@AdminOnly / @PreAuthorize on controllers) restricts mutations.
                // Don't "consolidate" into one approach without thinking — removing either half
                // either over-exposes new endpoints or scatters role checks into URL strings.
                .requestMatchers(HttpMethod.GET, "/api/exercises").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/exercises/{id:\\d+}").permitAll()
                .anyRequest().authenticated()
            )
            // Return 401 (not 403) when a request hits a protected endpoint with no token.
            // Spring Security's default entry point returns 403 for stateless setups.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
            // Allow H2 console to render (it uses iframes; Spring Security blocks them by default)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // Register our JWT filter to run before Spring's form-login filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
