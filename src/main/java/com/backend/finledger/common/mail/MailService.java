package com.backend.finledger.common.mail;

public interface MailService {

    /**
     * Sends an email with an optional binary attachment.
     *
     * @param to                 Recipient email address
     * @param subject            Email subject line
     * @param bodyHtml           HTML body content
     * @param attachmentFilename Attachment file name (e.g. INV-2026-0001.pdf)
     * @param attachmentBytes    Raw PDF/document bytes
     */
    void sendEmailWithAttachment(String to, String subject, String bodyHtml, String attachmentFilename, byte[] attachmentBytes);
}
