package com.eudext.erp.iam.internal.user;

import com.eudext.erp.iam.internal.password.PasswordPolicyService;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-1 / IAM-9: user provisioning and password changes, enforcing the tenant's password policy on every write. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository, PasswordPolicyService passwordPolicyService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(UUID tenantId, String email, String rawPassword) {
        return createUser(tenantId, email, rawPassword, false);
    }

    /** IAM-9: {@code mustChangePassword} is true for a system-generated credential (ADM-1/ADM-5 provisioning) so the first login forces a rotation. */
    @Transactional
    public User createUser(UUID tenantId, String email, String rawPassword, boolean mustChangePassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException(email);
        }
        passwordPolicyService.validate(tenantId, null, rawPassword);

        User user = User.create(tenantId, email, passwordEncoder.encode(rawPassword), mustChangePassword);
        User saved = userRepository.save(user);
        passwordPolicyService.recordHistory(tenantId, saved.getId(), saved.getPasswordHash());
        return saved;
    }

    @Transactional
    public void changePassword(UUID tenantId, UUID userId, String currentRawPassword, String newRawPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No such user"));
        if (!passwordEncoder.matches(currentRawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        passwordPolicyService.validate(tenantId, userId, newRawPassword);

        user.changePassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        passwordPolicyService.recordHistory(tenantId, userId, user.getPasswordHash());
    }
}
