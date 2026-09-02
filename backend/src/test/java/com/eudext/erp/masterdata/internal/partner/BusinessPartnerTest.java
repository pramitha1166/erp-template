package com.eudext.erp.masterdata.internal.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** MDM-5: customer/supplier master invariants. */
class BusinessPartnerTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @Test
    void createStartsEnabledWithZeroCreditLimit() {
        BusinessPartner partner = BusinessPartner.create(tenantId, companyId, BusinessPartnerType.CUSTOMER, "CUST-001", "Acme Traders");

        assertThat(partner.isDisabled()).isFalse();
        assertThat(partner.getCreditLimit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateDetailsNormalisesCreditLimitToScaleFour() {
        BusinessPartner partner = BusinessPartner.create(tenantId, companyId, BusinessPartnerType.SUPPLIER, "SUP-001", "Ceylon Supplies");

        partner.updateDetails(
                "Ceylon Supplies (Pvt) Ltd", "VAT-123", new BigDecimal("50000"), 30, null, "BOC", "Colombo", "0012345678", "BCEYLKLX");

        assertThat(partner.getCreditLimit()).isEqualByComparingTo(new BigDecimal("50000.0000"));
        assertThat(partner.getCreditLimit().scale()).isEqualTo(4);
    }

    @Test
    void rejectsNegativeCreditTermsDays() {
        BusinessPartner partner = BusinessPartner.create(tenantId, companyId, BusinessPartnerType.CUSTOMER, "CUST-002", "Beta Retail");

        assertThatThrownBy(() -> partner.updateDetails("Beta Retail", null, BigDecimal.ZERO, -1, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disableAndEnableToggleTheDisabledFlag() {
        BusinessPartner partner = BusinessPartner.create(tenantId, companyId, BusinessPartnerType.BOTH, "BP-001", "Both Trading");

        partner.disable();
        assertThat(partner.isDisabled()).isTrue();

        partner.enable();
        assertThat(partner.isDisabled()).isFalse();
    }
}
