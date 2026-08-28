package com.eudext.erp.iam.internal.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * IAM-1: a stateless JWT bearer-token API — no session cookies, no CSRF
 * (there's no cookie-based auth for CSRF to attack), no form login/HTTP
 * Basic. Paths are matched relative to {@code server.servlet.context-path}
 * (i.e. without the {@code /api} prefix).
 *
 * <p>CORS is enabled here so preflight {@code OPTIONS} requests are answered
 * before authentication rejects them; the policy itself lives in
 * {@code CorsConfig}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/auth/login", "/auth/refresh", "/auth/totp/verify",
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
        "/actuator/health", "/actuator/info",
        // ADM-1: one-time "first run creates the admin" bootstrap — see
        // PlatformBootstrapService's javadoc for why this is safe despite
        // being unauthenticated (it self-disables after the first admin
        // exists), and ADM-5's invite-acceptance flow, which is likewise
        // reached before the invitee has any session.
        "/admin/platform/bootstrap", "/admin/tenants/*/invites/accept"
    };

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
