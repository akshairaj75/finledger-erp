package com.backend.finledger.ledger;

import com.backend.finledger.ledger.dto.AccountDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDto toDto(Account account);
}

