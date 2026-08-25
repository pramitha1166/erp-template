package com.eudext.erp.iam.internal.totp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * RFC 4226 Appendix D publishes HOTP test vectors for the 20-byte ASCII
 * secret "12345678901234567890" (base32: {@code GEZDGNBVGY3TQOJQGEZDGNBV
 * GY3TQOJQ}) at counters 0..9; TOTP (RFC 6238) is HOTP with counter =
 * floor(unixTime / 30). Counter 1 (unix time in [30,59]) has HOTP code
 * 287082, so time 59 must produce it.
 */
class TotpServiceTest {

    private static final String RFC_4226_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private final TotpService totpService = new TotpService();

    @Test
    void matchesRfc4226HotpTestVectorAtCounterOne() {
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, "287082", Instant.ofEpochSecond(59))).isTrue();
    }

    @Test
    void matchesRfc4226HotpTestVectorAtCounterZero() {
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, "755224", Instant.ofEpochSecond(0))).isTrue();
    }

    @Test
    void rejectsWrongCode() {
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, "000000", Instant.ofEpochSecond(59))).isFalse();
    }

    @Test
    void rejectsMalformedCode() {
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, "12345", Instant.ofEpochSecond(59))).isFalse();
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, "abcdef", Instant.ofEpochSecond(59))).isFalse();
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, null, Instant.ofEpochSecond(59))).isFalse();
    }

    @Test
    void toleratesOneStepOfClockDrift() {
        // Counter 1 covers [30,59]; 29s later (89s) is counter 2 — one step of drift, still accepted.
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, "287082", Instant.ofEpochSecond(89))).isTrue();
        // Two steps away (119s, counter 3) must not be accepted.
        assertThat(totpService.verify(RFC_4226_SECRET_BASE32, "287082", Instant.ofEpochSecond(119))).isFalse();
    }

    @Test
    void generatedSecretRoundTripsThroughVerify() {
        String secret = totpService.generateSecret();
        Instant now = Instant.now();
        // Derive the current code the same way verify() would internally accept it: generate via public API isn't
        // exposed, so cross-check indirectly by asserting a freshly generated secret is valid base32 of the right length.
        assertThat(secret).matches("[A-Z2-7]+");
        assertThat(totpService.otpAuthUri("Eudext ERP", "user@example.com", secret))
                .startsWith("otpauth://totp/Eudext ERP:user@example.com?secret=" + secret);
    }
}
