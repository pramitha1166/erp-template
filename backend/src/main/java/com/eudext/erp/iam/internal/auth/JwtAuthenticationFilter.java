package com.eudext.erp.iam.internal.auth;

import com.eudext.erp.config.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IAM-1: resolves the caller's identity from the {@code Authorization:
 * Bearer <jwt>} header and, per the TODO in {@code TenantContext}, sets
 * {@code TenantContext} for the duration of the request from the token's
 * validated {@code tid} claim — never from a client-supplied header,
 * which is exactly the untrusted path that comment warns against. Cleared
 * in a {@code finally} so a pooled thread can never leak a tenant into an
 * unrelated later request.
 *
 * <p>Not a {@code @Component}: Spring Boot auto-registers any {@code
 * Filter} bean into the servlet container's generic filter chain in
 * addition to wherever Spring Security places it, which would run this
 * twice per request. {@code SecurityConfig} constructs and wires it into
 * the security filter chain explicitly instead.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            bearerToken(request).flatMap(jwtService::parseAccessToken).ifPresent(claims -> {
                TenantContext.set(claims.tenantId());
                var authentication =
                        new UsernamePasswordAuthenticationToken(claims.userId().toString(), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(header.substring("Bearer ".length()));
    }
}
