package com.backend.finledger.crm.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "leads", uniqueConstraints = {
    @UniqueConstraint(name = "uk_lead_no", columnNames = {"company_id", "lead_no"})
})
public class Lead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "lead_no", length = 30, nullable = false)
    private String leadNo;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "contact_name", length = 150, nullable = false)
    private String contactName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30, nullable = false)
    private LeadSource source = LeadSource.OTHER;

    @Column(name = "industry", length = 60)
    private String industry;

    @Column(name = "city", length = 60)
    private String city;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "converted_customer_id")
    private Long convertedCustomerId;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Lead() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getLeadNo() {
        return leadNo;
    }

    public void setLeadNo(String leadNo) {
        this.leadNo = leadNo;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LeadSource getSource() {
        return source;
    }

    public void setSource(LeadSource source) {
        this.source = source;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getConvertedCustomerId() {
        return convertedCustomerId;
    }

    public void setConvertedCustomerId(Long convertedCustomerId) {
        this.convertedCustomerId = convertedCustomerId;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

