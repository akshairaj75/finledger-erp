package com.backend.finledger.tax;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class TaxCalculator {

    public record CalculatedLine(
            BigDecimal lineSubtotal,
            BigDecimal discountAmount,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal lineTotal
    ) {}

    public record CalculatedDocumentTotals(
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal grandTotal
    ) {}

    public CalculatedLine calculateLine(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPct, BigDecimal taxRate) {
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ONE;
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        BigDecimal disc = discountPct != null ? discountPct : BigDecimal.ZERO;
        BigDecimal rate = taxRate != null ? taxRate : BigDecimal.ZERO;

        BigDecimal lineSubtotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = lineSubtotal.multiply(disc)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = lineSubtotal.subtract(discountAmount);
        BigDecimal taxAmount = netAmount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = netAmount.add(taxAmount);

        return new CalculatedLine(lineSubtotal, discountAmount, netAmount, taxAmount, lineTotal);
    }

    public CalculatedDocumentTotals calculateDocumentTotals(List<CalculatedLine> lines) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (lines != null) {
            for (CalculatedLine line : lines) {
                subtotal = subtotal.add(line.lineSubtotal());
                discountTotal = discountTotal.add(line.discountAmount());
                taxTotal = taxTotal.add(line.taxAmount());
                grandTotal = grandTotal.add(line.lineTotal());
            }
        }

        return new CalculatedDocumentTotals(subtotal, discountTotal, taxTotal, grandTotal);
    }
}

