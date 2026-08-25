package com.eudext.erp.iam.internal.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.eudext.erp.iam.internal.settings.SecurityPolicy;
import com.eudext.erp.iam.internal.settings.TenantSecuritySettingsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private UserSessionRepository repository;

    @Mock
    private TenantSecuritySettingsService settingsService;

    private SessionService service;
    private final ArgumentCaptor<UserSession> savedSessions = ArgumentCaptor.forClass(UserSession.class);
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SessionService(repository, settingsService);
        lenient().when(settingsService.resolve(any())).thenReturn(SecurityPolicy.defaults());
        // Mimics JPA's GenerationType.UUID assigning an id at save() time, which a bare mock otherwise leaves null —
        // markRotatedTo()/wasRotated() depend on the rotated-to session having a real id.
        lenient().when(repository.save(savedSessions.capture())).thenAnswer(inv -> {
            UserSession session = inv.getArgument(0);
            assignIdIfMissing(session);
            return session;
        });
    }

    private void assignIdIfMissing(UserSession session) {
        if (session.getId() != null) {
            return;
        }
        try {
            var field = UserSession.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(session, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void extractTenantIdReadsThePrefixEmbeddedInTheRawToken() {
        SessionService.IssuedSession issued = service.issue(tenantId, userId, "127.0.0.1", "junit", Duration.ofDays(7));
        assertThat(SessionService.extractTenantId(issued.rawRefreshToken())).isEqualTo(tenantId);
    }

    @Test
    void extractTenantIdRejectsATokenWithoutAValidPrefix() {
        assertThatThrownBy(() -> SessionService.extractTenantId("not-a-valid-token")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotatingAFreshTokenSucceedsAndRevokesTheOldOne() {
        SessionService.IssuedSession issued = service.issue(tenantId, userId, "127.0.0.1", "junit", Duration.ofDays(7));
        UserSession original = savedSessions.getValue();
        when(repository.findByRefreshTokenHash(hash(issued.rawRefreshToken()))).thenReturn(Optional.of(original));

        SessionService.RotationResult result = service.rotate(issued.rawRefreshToken(), "127.0.0.1", "junit", Duration.ofDays(7));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(original.isRevoked()).isTrue();
        assertThat(original.wasRotated()).isTrue();
    }

    @Test
    void presentingAnAlreadyRotatedTokenIsDetectedAsReuseAndRevokesEverySession() {
        SessionService.IssuedSession first = service.issue(tenantId, userId, "127.0.0.1", "junit", Duration.ofDays(7));
        UserSession original = savedSessions.getValue();
        when(repository.findByRefreshTokenHash(hash(first.rawRefreshToken()))).thenReturn(Optional.of(original));
        service.rotate(first.rawRefreshToken(), "127.0.0.1", "junit", Duration.ofDays(7));

        // Re-present the same (now-rotated) token — simulates a stolen refresh token being replayed.
        when(repository.findByUserId(userId)).thenReturn(savedSessions.getAllValues());

        assertThatThrownBy(() -> service.rotate(first.rawRefreshToken(), "10.0.0.1", "attacker", Duration.ofDays(7)))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        assertThat(savedSessions.getAllValues()).allMatch(UserSession::isRevoked);
    }

    @Test
    void invalidTokenRaisesInvalidRefreshTokenException() {
        when(repository.findByRefreshTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate(tenantId + ":bogus", "127.0.0.1", "junit", Duration.ofDays(7)))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
