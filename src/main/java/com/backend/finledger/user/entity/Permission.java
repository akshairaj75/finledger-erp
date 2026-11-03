package com.backend.finledger.user.entity;

import jakarta.persistence.*;

/**
 * Permission entity matching permissions table.
 */
@Entity
@Table(name = "permissions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_permissions_code", columnNames = {"code"})
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 80, nullable = false)
    private String code;

    @Column(name = "module", length = 40, nullable = false)
    private String module;

    @Column(name = "description", length = 255)
    private String description;

    public Permission() {}

    public Permission(String code, String module, String description) {
        this.code = code;
        this.module = module;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

