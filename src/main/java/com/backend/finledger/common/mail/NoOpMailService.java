package com.backend.finledger.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-Op MailService implementation used in development, testing, and environments
 * where finledger.mail.enabled is false or unset.
 */
@Service
@ConditionalOnProperty(name = "finledger.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpMailService.class);

    @Override
    public void sendEmailWithAttachment(String to, String subject, String bodyHtml, String attachmentFilename, byte[] attachmentBytes) {
        int attachmentSize = (attachmentBytes != null) ? attachmentBytes.length : 0;
        log.info("[NoOpMailService] Mail dispatch simulated. To: '{}', Subject: '{}', Attachment: '{}' ({} bytes)",
                to, subject, attachmentFilename, attachmentSize);
    }
}
