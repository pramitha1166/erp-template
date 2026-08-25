package com.eudext.erp.iam.internal.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.eudext.erp.iam.internal.settings.SecurityPolicy;
import com.eudext.erp.iam.internal.settings.TenantSecuritySettingsService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock
    private TenantSecuritySettingsService settingsService;

    @Mock
    private PasswordHistoryRepository historyRepository;

    private final PasswordEncoder passwordEncoder = org.springframework.security.crypto.factory.PasswordEncoderFactories
            .createDelegatingPasswordEncoder();

    private PasswordPolicyService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PasswordPolicyService(settingsService, historyRepository, passwordEncoder);
    }

    @Test
    void acceptsAPasswordMeetingDefaultPolicy() {
        when(settingsService.resolve(tenantId)).thenReturn(SecurityPolicy.defaults());
        when(historyRepository.findRecentByUserId(any())).thenReturn(List.of());

        service.validate(tenantId, UUID.randomUUID(), "Str0ngPassw0rd");
    }

    @Test
    void rejectsAPasswordShorterThanMinLength() {
        when(settingsService.resolve(tenantId)).thenReturn(SecurityPolicy.defaults());
        when(historyRepository.findRecentByUserId(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.validate(tenantId, UUID.randomUUID(), "Sh0rt"))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void rejectsAPasswordMissingRequiredComplexity() {
        when(settingsService.resolve(tenantId)).thenReturn(SecurityPolicy.defaults());
        when(historyRepository.findRecentByUserId(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.validate(tenantId, UUID.randomUUID(), "alllowercase123"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .satisfies(e -> assertThat(((PasswordPolicyViolationException) e).getViolations())
                        .anyMatch(v -> v.contains("uppercase")));
    }

    @Test
    void rejectsReuseOfARecentPassword() {
        UUID userId = UUID.randomUUID();
        when(settingsService.resolve(tenantId)).thenReturn(SecurityPolicy.defaults());
        String encoded = passwordEncoder.encode("Str0ngPassw0rd");
        PasswordHistory history = PasswordHistory.of(tenantId, userId, encoded);
        when(historyRepository.findRecentByUserId(userId)).thenReturn(List.of(history));

        assertThatThrownBy(() -> service.validate(tenantId, userId, "Str0ngPassw0rd"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .satisfies(e -> assertThat(((PasswordPolicyViolationException) e).getViolations())
                        .anyMatch(v -> v.contains("reused")));
    }

    @Test
    void allowsAPasswordNotInHistory() {
        UUID userId = UUID.randomUUID();
        when(settingsService.resolve(tenantId)).thenReturn(SecurityPolicy.defaults());
        String encoded = passwordEncoder.encode("OldPassw0rd1");
        when(historyRepository.findRecentByUserId(userId)).thenReturn(List.of(PasswordHistory.of(tenantId, userId, encoded)));

        service.validate(tenantId, userId, "BrandNewPassw0rd");
    }

    @Test
    void isExpiredHonorsExpiryDaysConfiguration() {
        SecurityPolicy neverExpires = SecurityPolicy.defaults();
        assertThat(service.isExpired(neverExpires, java.time.Instant.now().minusSeconds(1_000_000))).isFalse();

        SecurityPolicy expiresIn90Days = new SecurityPolicy(30, 10, true, true, true, false, 3, 90);
        assertThat(service.isExpired(expiresIn90Days, java.time.Instant.now().minus(91, java.time.temporal.ChronoUnit.DAYS)))
                .isTrue();
        assertThat(service.isExpired(expiresIn90Days, java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS)))
                .isFalse();
    }
}
