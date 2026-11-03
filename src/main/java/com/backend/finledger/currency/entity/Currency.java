package com.backend.finledger.currency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Currency entity matching currencies table.
 */
@Entity
@Table(name = "currencies")
public class Currency {

    @Id
    @Column(name = "code", length = 3, nullable = false)
    private String code;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "symbol", length = 5)
    private String symbol;

    @Column(name = "decimal_places", nullable = false, columnDefinition = "tinyint")
    private Byte decimalPlaces = 2;

    public Currency() {}

    public Currency(String code, String name, String symbol, Byte decimalPlaces) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Byte getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Byte decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }
}

