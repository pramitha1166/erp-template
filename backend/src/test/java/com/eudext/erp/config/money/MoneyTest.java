package com.eudext.erp.config.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void scalesToFourDecimalPlacesHalfUp() {
        assertThat(Money.scale(new BigDecimal("1.00005"))).isEqualByComparingTo("1.0001");
        assertThat(Money.scale(new BigDecimal("1.00004"))).isEqualByComparingTo("1.0000");
    }

    @Test
    void zeroIsScaledToFourDecimalPlaces() {
        assertThat(Money.zero().scale()).isEqualTo(4);
        assertThat(Money.zero()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void arithmeticStaysAtScaleFour() {
        BigDecimal sum = Money.add(new BigDecimal("10.1"), new BigDecimal("0.05"));
        BigDecimal difference = Money.subtract(sum, new BigDecimal("0.15"));
        BigDecimal product = Money.multiply(new BigDecimal("2"), new BigDecimal("3.33335"));

        assertThat(sum).isEqualByComparingTo("10.1500").matches(v -> v.scale() == 4);
        assertThat(difference).isEqualByComparingTo("10.0000").matches(v -> v.scale() == 4);
        assertThat(product).isEqualByComparingTo("6.6668").matches(v -> v.scale() == 4);
    }
}
