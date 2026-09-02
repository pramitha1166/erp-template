package com.eudext.erp.masterdata.internal.partner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** MDM-5: one contact person for a {@link BusinessPartner} — a partner may have several. */
@Entity
@Table(name = "business_partner_contacts")
@EntityListeners(AuditingEntityListener.class)
public class BusinessPartnerContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "designation")
    private String designation;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected BusinessPartnerContact() {}

    public static BusinessPartnerContact create(
            UUID tenantId, UUID partnerId, String name, String designation, String phone, String email, boolean primaryContact) {
        BusinessPartnerContact contact = new BusinessPartnerContact();
        contact.tenantId = tenantId;
        contact.partnerId = partnerId;
        contact.name = name;
        contact.designation = designation;
        contact.phone = phone;
        contact.email = email;
        contact.primaryContact = primaryContact;
        return contact;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public String getName() {
        return name;
    }

    public String getDesignation() {
        return designation;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }
}
