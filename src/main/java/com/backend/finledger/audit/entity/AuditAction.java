package com.backend.finledger.audit.entity;

/**
 * Audit actions matching the enum in audit_logs table schema.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    POST,
    VOID,
    APPROVE,
    REJECT,
    CONVERT,
    LOGIN
}

