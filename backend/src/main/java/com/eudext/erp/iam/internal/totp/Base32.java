package com.eudext.erp.iam.internal.totp;

/**
 * RFC 4648 base32 codec (no padding on encode; padding tolerated on
 * decode). TOTP secrets are conventionally shown to users as base32 so
 * they can be typed into an authenticator app, so this is used purely for
 * that display/parse round-trip, not as a security primitive.
 */
final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {}

    static String encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsInBuffer = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsInBuffer += 8;
            while (bitsInBuffer >= 5) {
                int index = (buffer >> (bitsInBuffer - 5)) & 0x1F;
                result.append(ALPHABET.charAt(index));
                bitsInBuffer -= 5;
            }
        }
        if (bitsInBuffer > 0) {
            int index = (buffer << (5 - bitsInBuffer)) & 0x1F;
            result.append(ALPHABET.charAt(index));
        }
        return result.toString();
    }

    static byte[] decode(String encoded) {
        String cleaned = encoded.trim().toUpperCase().replace("=", "");
        byte[] result = new byte[cleaned.length() * 5 / 8];
        int buffer = 0;
        int bitsInBuffer = 0;
        int resultIndex = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            int value = ALPHABET.indexOf(cleaned.charAt(i));
            if (value < 0) {
                throw new IllegalArgumentException("Invalid base32 character: " + cleaned.charAt(i));
            }
            buffer = (buffer << 5) | value;
            bitsInBuffer += 5;
            if (bitsInBuffer >= 8) {
                result[resultIndex++] = (byte) ((buffer >> (bitsInBuffer - 8)) & 0xFF);
                bitsInBuffer -= 8;
            }
        }
        return result;
    }
}
