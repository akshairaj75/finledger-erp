package com.backend.finledger.common;

/**
 * Supported document types for number series generation matching the number_series table schema.
 */
public enum DocType {
    JOURNAL,
    INVOICE,
    BILL,
    RECEIPT,
    PAYMENT,
    CREDIT_NOTE,
    QUOTATION,
    LEAD,
    OPPORTUNITY,
    EXPENSE,
    ADJUSTMENT,
    TRANSFER
}

