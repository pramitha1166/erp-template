package com.eudext.erp.iam.internal.auth;

import com.eudext.erp.config.tenancy.ImpersonationContext;
import com.eudext.erp.config.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
            Optional<String> token = bearerToken(request);
            token.flatMap(jwtService::parseAccessToken)
                    .ifPresentOrElse(
                            claims -> authenticate(claims.userId(), claims.tenantId(), null),
                            () -> token.flatMap(jwtService::parseImpersonationToken)
                                    .ifPresent(claims -> authenticate(
                                            claims.targetUserId(), claims.targetTenantId(), claims.actorUserId())));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            ImpersonationContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * ADM-7: {@code actorUserId} is non-null only for an impersonation
     * token — the authenticated principal is still {@code userId} (the
     * impersonated tenant-admin) so every downstream permission check
     * behaves exactly as it would for that user's own login.
     */
    private void authenticate(UUID userId, UUID tenantId, UUID actorUserId) {
        TenantContext.set(tenantId);
        ImpersonationContext.set(actorUserId);
        var authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(header.substring("Bearer ".length()));
    }
}
