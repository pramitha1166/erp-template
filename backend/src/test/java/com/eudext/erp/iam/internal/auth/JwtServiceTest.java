package com.eudext.erp.iam.internal.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtProperties properties =
            new JwtProperties("test-signing-secret-at-least-32-bytes-long!!", Duration.ofMinutes(15), Duration.ofDays(7));
    private final JwtService jwtService = new JwtService(properties);

    @Test
    void issuedAccessTokenParsesBackToTheSameClaims() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtService.issueAccessToken(userId, tenantId);
        JwtService.AccessTokenClaims claims = jwtService.parseAccessToken(token).orElseThrow();

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void rejectsGarbageTokens() {
        assertThat(jwtService.parseAccessToken("not-a-jwt")).isEmpty();
    }

    @Test
    void mfaChallengeTokenIsNotAcceptedAsAnAccessToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String mfaToken = jwtService.issueMfaChallenge(userId, tenantId);

        assertThat(jwtService.parseAccessToken(mfaToken)).isEmpty();
        assertThat(jwtService.parseMfaChallenge(mfaToken)).isPresent();
    }

    @Test
    void accessTokenIsNotAcceptedAsAnMfaChallenge() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String accessToken = jwtService.issueAccessToken(userId, tenantId);

        assertThat(jwtService.parseMfaChallenge(accessToken)).isEmpty();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtService other = new JwtService(
                new JwtProperties("a-completely-different-signing-secret-32bytes", Duration.ofMinutes(15), Duration.ofDays(7)));
        String token = other.issueAccessToken(UUID.randomUUID(), UUID.randomUUID());

        assertThat(jwtService.parseAccessToken(token)).isEmpty();
    }

    @Test
    void issuedImpersonationTokenParsesBackToActorAndTargetSeparately() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID targetTenantId = UUID.randomUUID();

        String token = jwtService.issueImpersonationToken(actorUserId, targetUserId, targetTenantId, Duration.ofMinutes(30));
        JwtService.ImpersonationClaims claims = jwtService.parseImpersonationToken(token).orElseThrow();

        assertThat(claims.actorUserId()).isEqualTo(actorUserId);
        assertThat(claims.targetUserId()).isEqualTo(targetUserId);
        assertThat(claims.targetTenantId()).isEqualTo(targetTenantId);
    }

    /** ADM-7: an impersonation token must never be usable as if it were a real login for the acting admin's own identity. */
    @Test
    void impersonationTokenIsNotAcceptedAsAnAccessToken() {
        String token = jwtService.issueImpersonationToken(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Duration.ofMinutes(30));

        assertThat(jwtService.parseAccessToken(token)).isEmpty();
    }

    @Test
    void accessTokenIsNotAcceptedAsAnImpersonationToken() {
        String accessToken = jwtService.issueAccessToken(UUID.randomUUID(), UUID.randomUUID());

        assertThat(jwtService.parseImpersonationToken(accessToken)).isEmpty();
    }
}
