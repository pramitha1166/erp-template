package com.eudext.erp.iam.internal.totp;

import com.eudext.erp.iam.internal.user.User;
import com.eudext.erp.iam.internal.user.UserRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-2: the enrollment half of TOTP — setup (generate a candidate secret) and enable (confirm it with a code). */
@Service
public class TotpEnrollmentService {

    private static final String ISSUER = "Eudext ERP";

    private final UserRepository userRepository;
    private final TotpService totpService;

    public TotpEnrollmentService(UserRepository userRepository, TotpService totpService) {
        this.userRepository = userRepository;
        this.totpService = totpService;
    }

    public record EnrollmentStart(String secret, String otpAuthUri) {}

    @Transactional
    public EnrollmentStart beginEnrollment(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No such user"));
        String secret = totpService.generateSecret();
        user.beginTotpEnrollment(secret);
        userRepository.save(user);
        return new EnrollmentStart(secret, totpService.otpAuthUri(ISSUER, user.getEmail(), secret));
    }

    @Transactional
    public void confirmEnrollment(UUID userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No such user"));
        if (user.getTotpSecret() == null || !totpService.verify(user.getTotpSecret(), code)) {
            throw new InvalidTotpCodeException();
        }
        user.confirmTotpEnrollment();
        userRepository.save(user);
    }

    @Transactional
    public void disable(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No such user"));
        user.disableTotp();
        userRepository.save(user);
    }
}
