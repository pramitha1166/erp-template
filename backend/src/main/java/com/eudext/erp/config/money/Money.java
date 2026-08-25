package com.eudext.erp.config.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * ARCH-5: every monetary value is a {@link BigDecimal} at scale 4
 * internally. These are the shared conventions every module uses instead
 * of each hand-rolling its own scale/rounding rules — currency-precision
 * display formatting is a UI/reporting concern, not this class's job.
 */
public final class Money {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {}

    /** Normalises {@code value} to scale 4, half-up. */
    public static BigDecimal scale(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return scale(scale(a).add(scale(b)));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return scale(scale(a).subtract(scale(b)));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return scale(scale(a).multiply(scale(b)));
    }
}
