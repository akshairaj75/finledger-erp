package com.backend.finledger.expense.service;

import com.backend.finledger.expense.dto.BatchReimburseRequest;
import com.backend.finledger.expense.dto.BatchReimburseResponse;

public interface ReimbursementService {

    /**
     * Batch reimbursement settling multiple approved/posted employee claims.
     * Posts: Dr Employee Reimbursements Payable, Cr Bank/Cash.
     */
    BatchReimburseResponse reimburseExpenses(BatchReimburseRequest request);
}

