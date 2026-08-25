package com.eudext.erp.iam.internal.password;

import com.eudext.erp.iam.internal.settings.SecurityPolicy;
import com.eudext.erp.iam.internal.settings.TenantSecuritySettingsService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM-9: enforces a tenant's configured password length/complexity/history
 * requirements, and reports expiry so callers (login, "change password")
 * can force a rotation.
 */
@Service
public class PasswordPolicyService {

    /** How many recent hashes to check even if a tenant's historyCount is configured higher; keeps the query bounded. */
    private static final int MAX_HISTORY_CHECKED = 24;

    private final TenantSecuritySettingsService settingsService;
    private final PasswordHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyService(
            TenantSecuritySettingsService settingsService,
            PasswordHistoryRepository historyRepository,
            PasswordEncoder passwordEncoder) {
        this.settingsService = settingsService;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates a candidate raw password against the tenant's policy,
     * including reuse against {@code userId}'s recent password history.
     * Pass {@code userId == null} when there is no user yet (registration).
     *
     * @throws PasswordPolicyViolationException if any rule is violated
     */
    @Transactional(readOnly = true)
    public void validate(UUID tenantId, UUID userId, String rawPassword) {
        SecurityPolicy policy = settingsService.resolve(tenantId);
        List<String> violations = complexityViolations(policy, rawPassword);

        if (userId != null && policy.historyCount() > 0) {
            List<PasswordHistory> recent = historyRepository.findRecentByUserId(userId);
            int checked = 0;
            for (PasswordHistory entry : recent) {
                if (checked >= policy.historyCount() || checked >= MAX_HISTORY_CHECKED) {
                    break;
                }
                if (passwordEncoder.matches(rawPassword, entry.getPasswordHash())) {
                    violations.add("password was used recently and cannot be reused");
                    break;
                }
                checked++;
            }
        }

        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
    }

    public boolean isExpired(SecurityPolicy policy, Instant passwordChangedAt) {
        if (policy.expiryDays() == null) {
            return false;
        }
        return passwordChangedAt.plus(policy.expiryDays(), ChronoUnit.DAYS).isBefore(Instant.now());
    }

    @Transactional
    public void recordHistory(UUID tenantId, UUID userId, String passwordHash) {
        historyRepository.save(PasswordHistory.of(tenantId, userId, passwordHash));
    }

    private List<String> complexityViolations(SecurityPolicy policy, String rawPassword) {
        List<String> violations = new ArrayList<>();
        if (rawPassword == null || rawPassword.length() < policy.minLength()) {
            violations.add("must be at least " + policy.minLength() + " characters");
        }
        if (rawPassword == null) {
            return violations;
        }
        if (policy.requireUpper() && rawPassword.chars().noneMatch(Character::isUpperCase)) {
            violations.add("must contain an uppercase letter");
        }
        if (policy.requireLower() && rawPassword.chars().noneMatch(Character::isLowerCase)) {
            violations.add("must contain a lowercase letter");
        }
        if (policy.requireDigit() && rawPassword.chars().noneMatch(Character::isDigit)) {
            violations.add("must contain a digit");
        }
        if (policy.requireSymbol() && rawPassword.chars().allMatch(Character::isLetterOrDigit)) {
            violations.add("must contain a symbol");
        }
        return violations;
    }
}
