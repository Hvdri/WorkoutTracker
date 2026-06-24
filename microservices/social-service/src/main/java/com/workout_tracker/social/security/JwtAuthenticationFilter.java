package com.workout_tracker.social.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

// Verifies JWTs signed by the monolith using the shared HMAC secret. No DB lookup
// here: everything the social-service needs (userId, roles) is carried in the token
// claims, so we can authenticate without owning a copy of the user table.
//
// Deviation from awbd2026/product-hub which uses oauth2-resource-server pointed at
// Keycloak JWKs. We document this trade-off in the README — it's "shared symmetric
// secret" instead of "asymmetric JWKs from an identity provider." Same trust model,
// less infra.
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Decoders.BASE64URL.decode(jwtProperties.getSecret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET decodes to only " + keyBytes.length + " bytes. " +
                "Minimum is 32 bytes (256 bits) for HMAC-SHA256.");
        }
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Subject = username; userId is a custom claim added by the monolith's
            // JwtService.generateToken(UserDetails, Long). Bail if it's missing —
            // means we're talking to an old token issued before the migration; the
            // user just needs to log in again.
            String username = claims.getSubject();
            Object userIdRaw = claims.get("userId");
            if (userIdRaw == null) {
                log.warn("JWT missing userId claim — token issued before social-service migration");
                filterChain.doFilter(request, response);
                return;
            }
            Long userId = ((Number) userIdRaw).longValue();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());

            AuthenticatedUser principal = new AuthenticatedUser(userId, username, roles);
            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException ex) {
            // Same posture as the monolith — bad token = anonymous, the next filter
            // in the chain (or the auth entry point) will return 401.
            log.warn("Invalid JWT: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
