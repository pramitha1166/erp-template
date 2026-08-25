package com.eudext.erp.iam.internal.totp;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.InvalidKeyException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * IAM-2: RFC 6238 TOTP (HMAC-SHA1, 30s step, 6 digits) — the same algorithm
 * Google Authenticator / Authy / 1Password implement, so any of those apps
 * can scan the {@link #otpAuthUri} and produce matching codes. Hand-rolled
 * rather than pulling in a third TOTP dependency: the algorithm is a short,
 * fully-specified RFC with stable test vectors (RFC 4226 Appendix D), so it
 * is easy to verify correct here and keeps the dependency surface smaller.
 */
@Service
public class TotpService {

    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    /** Tolerate one step of clock drift either side, per common TOTP practice. */
    private static final int ALLOWED_STEP_DRIFT = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secret);
        return Base32.encode(secret);
    }

    public String otpAuthUri(String issuer, String accountEmail, String base32Secret) {
        return "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d"
                .formatted(issuer, accountEmail, base32Secret, issuer, DIGITS, TIME_STEP_SECONDS);
    }

    public boolean verify(String base32Secret, String code) {
        return verify(base32Secret, code, Instant.now());
    }

    boolean verify(String base32Secret, String code, Instant at) {
        if (code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        long currentStep = at.getEpochSecond() / TIME_STEP_SECONDS;
        for (int drift = -ALLOWED_STEP_DRIFT; drift <= ALLOWED_STEP_DRIFT; drift++) {
            if (code.equals(generateCode(base32Secret, currentStep + drift))) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String base32Secret, long step) {
        byte[] key = Base32.decode(base32Secret);
        byte[] stepBytes = ByteBuffer.allocate(8).putLong(step).array();
        byte[] hash = hmacSha1(key, stepBytes);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, DIGITS);
        return String.format("%0" + DIGITS + "d", otp);
    }

    private byte[] hmacSha1(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA1 unavailable", e);
        }
    }
}
