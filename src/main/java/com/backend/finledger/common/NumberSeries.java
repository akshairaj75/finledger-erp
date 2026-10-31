package com.backend.finledger.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * NumberSeries entity matching the number_series table schema.
 */
@Entity
@Table(name = "number_series", uniqueConstraints = {
    @UniqueConstraint(name = "uk_series", columnNames = {"company_id", "doc_type"})
})
public class NumberSeries extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private DocType docType;

    @Column(name = "prefix", nullable = false, length = 20)
    private String prefix;

    @Column(name = "next_number", nullable = false)
    private Integer nextNumber = 1;

    @Column(name = "padding", nullable = false, columnDefinition = "tinyint")
    private Byte padding = 4;

    public NumberSeries() {
    }

    public NumberSeries(Long companyId, DocType docType, String prefix, Integer nextNumber, Byte padding) {
        this.companyId = companyId;
        this.docType = docType;
        this.prefix = prefix;
        this.nextNumber = nextNumber;
        this.padding = padding;
    }

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

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Integer getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(Integer nextNumber) {
        this.nextNumber = nextNumber;
    }

    public Byte getPadding() {
        return padding;
    }

    public void setPadding(Byte padding) {
        this.padding = padding;
    }
}
