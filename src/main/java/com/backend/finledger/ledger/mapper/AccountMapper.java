package com.backend.finledger.ledger.mapper;

import com.backend.finledger.ledger.dto.AccountDto;
import com.backend.finledger.ledger.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDto toDto(Account account);
}

