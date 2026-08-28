package com.eudext.erp.iam.internal.provisioning;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ADM-2: a one-time password handed to a freshly provisioned tenant-admin
 * (via the invite/welcome notification, never logged). Composed from every
 * character class {@code PasswordPolicyService} can require, so it clears
 * any tenant's configured complexity policy regardless of settings, rather
 * than gambling on plain random bytes happening to satisfy it.
 */
final class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*-_=+";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {}

    static String generate() {
        List<Character> chars = new ArrayList<>();
        chars.add(pick(UPPER));
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(DIGITS));
        chars.add(pick(SYMBOLS));
        chars.add(pick(SYMBOLS));
        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        for (int i = chars.size(); i < 20; i++) {
            chars.add(pick(all));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder builder = new StringBuilder(chars.size());
        chars.forEach(builder::append);
        return builder.toString();
    }

    private static char pick(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }
}
