package com.finledger.backend.common;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeries;
import com.backend.finledger.common.NumberSeriesRepository;
import java.time.Year;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional service using SELECT ... FOR UPDATE on number_series table
 * to generate gap-free document numbers (e.g. INV-2026-0001) scoped per company_id and doc_type.
 */
@Service
public class NumberSeriesService {

    private static final Logger log = LoggerFactory.getLogger(NumberSeriesService.class);

    private final NumberSeriesRepository numberSeriesRepository;

    public NumberSeriesService(NumberSeriesRepository numberSeriesRepository) {
        this.numberSeriesRepository = numberSeriesRepository;
    }

    /**
     * Generates the next gap-free document number inside an active transaction.
     * Uses pessimistic write lock (SELECT ... FOR UPDATE) to avoid sequence race conditions.
     *
     * @param companyId Tenant company ID
     * @param docType   Document type
     * @return Formatted document number, e.g. "INV-2026-0001"
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String generateNextNumber(Long companyId, DocType docType) {
        if (companyId == null) {
            throw new BusinessException("COMPANY_REQUIRED", "Company ID is required to generate document number");
        }
        if (docType == null) {
            throw new BusinessException("DOC_TYPE_REQUIRED", "Document type is required to generate document number");
        }

        NumberSeries series = numberSeriesRepository.findByCompanyIdAndDocTypeForUpdate(companyId, docType)
                .orElseGet(() -> createDefaultSeries(companyId, docType));

        int currentNumber = series.getNextNumber() != null ? series.getNextNumber() : 1;
        int padding = (series.getPadding() != null && series.getPadding() > 0) ? series.getPadding() : 4;

        String currentYear = String.valueOf(Year.now().getValue());
        String currentShortYear = String.format("%02d", Year.now().getValue() % 100);

        String prefix = series.getPrefix() != null ? series.getPrefix() : getDefaultPrefix(docType);
        String resolvedPrefix = prefix
                .replace("{YYYY}", currentYear)
                .replace("{YY}", currentShortYear);

        String formattedSeq = String.format("%0" + padding + "d", currentNumber);
        String documentNumber = resolvedPrefix + formattedSeq;

        series.setNextNumber(currentNumber + 1);
        numberSeriesRepository.save(series);

        log.debug("Generated doc number [{}] for company [{}] and doc_type [{}]", documentNumber, companyId, docType);
        return documentNumber;
    }

    /**
     * String-based convenience overload.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String generateNextNumber(Long companyId, String docTypeStr) {
        try {
            DocType docType = DocType.valueOf(docTypeStr.toUpperCase());
            return generateNextNumber(companyId, docType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_DOC_TYPE", "Invalid document type: " + docTypeStr);
        }
    }

    private NumberSeries createDefaultSeries(Long companyId, DocType docType) {
        String defaultPrefix = getDefaultPrefix(docType);
        NumberSeries newSeries = new NumberSeries(companyId, docType, defaultPrefix, 1, (byte) 4);
        return numberSeriesRepository.save(newSeries);
    }

    public static String getDefaultPrefix(DocType docType) {
        return switch (docType) {
            case INVOICE -> "INV-{YYYY}-";
            case JOURNAL -> "JV-{YYYY}-";
            case BILL -> "BILL-{YYYY}-";
            case RECEIPT -> "RCT-{YYYY}-";
            case PAYMENT -> "PAY-{YYYY}-";
            case CREDIT_NOTE -> "CN-{YYYY}-";
            case QUOTATION -> "QT-{YYYY}-";
            case LEAD -> "LEAD-{YYYY}-";
            case OPPORTUNITY -> "OPP-{YYYY}-";
            case EXPENSE -> "EXP-{YYYY}-";
            case ADJUSTMENT -> "ADJ-{YYYY}-";
            case TRANSFER -> "TRF-{YYYY}-";
        };
    }
}
