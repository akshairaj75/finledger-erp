package com.backend.finledger.ledger.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.ledger.dto.CostCenterDto;
import com.backend.finledger.ledger.dto.CostCenterRequest;
import com.backend.finledger.ledger.entity.CostCenter;
import com.backend.finledger.ledger.mapper.LedgerMapper;
import com.backend.finledger.ledger.repository.CostCenterRepository;
import com.backend.finledger.ledger.service.CostCenterService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CostCenterServiceImpl implements CostCenterService {

    private final CostCenterRepository costCenterRepository;
    private final LedgerMapper ledgerMapper;

    public CostCenterServiceImpl(CostCenterRepository costCenterRepository, LedgerMapper ledgerMapper) {
        this.costCenterRepository = costCenterRepository;
        this.ledgerMapper = ledgerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_VIEW') or hasAuthority('SETTINGS_EDIT') or hasAuthority('ACCOUNT_VIEW') or isAuthenticated()")
    public List<CostCenterDto> getCostCenters(Boolean activeOnly) {
        Long companyId = CurrentUser.getCompanyId();
        List<CostCenter> list = Boolean.TRUE.equals(activeOnly)
                ? costCenterRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId)
                : costCenterRepository.findByCompanyIdOrderByNameAsc(companyId);
        return ledgerMapper.toCostCenterDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_VIEW') or hasAuthority('SETTINGS_EDIT') or hasAuthority('ACCOUNT_VIEW') or isAuthenticated()")
    public CostCenterDto getCostCenterById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        CostCenter cc = costCenterRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("COST_CENTER_NOT_FOUND", "Cost center not found with id: " + id));
        return ledgerMapper.toCostCenterDto(cc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public CostCenterDto createCostCenter(CostCenterRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (costCenterRepository.findByCompanyIdAndCode(companyId, request.code()).isPresent()) {
            throw new BusinessException("COST_CENTER_EXISTS", "Cost center with code already exists: " + request.code());
        }

        if (request.parentId() != null) {
            costCenterRepository.findByCompanyIdAndId(companyId, request.parentId())
                    .orElseThrow(() -> new BusinessException("PARENT_NOT_FOUND", "Parent cost center not found with id: " + request.parentId()));
        }

        CostCenter cc = new CostCenter(companyId, request.code(), request.name(), request.parentId());
        if (request.isActive() != null) {
            cc.setIsActive(request.isActive());
        }

        CostCenter saved = costCenterRepository.save(cc);
        return ledgerMapper.toCostCenterDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public CostCenterDto updateCostCenter(Long id, CostCenterRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        CostCenter cc = costCenterRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("COST_CENTER_NOT_FOUND", "Cost center not found with id: " + id));

        if (!cc.getCode().equalsIgnoreCase(request.code())) {
            if (costCenterRepository.findByCompanyIdAndCode(companyId, request.code()).isPresent()) {
                throw new BusinessException("COST_CENTER_EXISTS", "Cost center with code already exists: " + request.code());
            }
            cc.setCode(request.code());
        }

        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new BusinessException("INVALID_PARENT", "Cost center cannot be its own parent");
            }
            costCenterRepository.findByCompanyIdAndId(companyId, request.parentId())
                    .orElseThrow(() -> new BusinessException("PARENT_NOT_FOUND", "Parent cost center not found with id: " + request.parentId()));
        }

        cc.setName(request.name());
        cc.setParentId(request.parentId());
        if (request.isActive() != null) {
            cc.setIsActive(request.isActive());
        }

        CostCenter saved = costCenterRepository.save(cc);
        return ledgerMapper.toCostCenterDto(saved);
    }
}

