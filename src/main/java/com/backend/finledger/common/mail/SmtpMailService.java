package com.backend.finledger.common.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Live SMTP MailService implementation active when finledger.mail.enabled=true.
 */
@Service
@ConditionalOnProperty(name = "finledger.mail.enabled", havingValue = "true")
public class SmtpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);

    private final JavaMailSender mailSender;

    public SmtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String bodyHtml, String attachmentFilename, byte[] attachmentBytes) {
        if (to == null || to.isBlank()) {
            log.warn("Cannot send email: recipient address is empty or null");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);

            if (attachmentFilename != null && attachmentBytes != null && attachmentBytes.length > 0) {
                helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentBytes));
            }

            mailSender.send(message);
            log.info("Sent email successfully to '{}' with attachment '{}'", to, attachmentFilename);
        } catch (Exception e) {
            log.error("Failed to deliver email to '{}': {}", to, e.getMessage(), e);
        }
    }
}
