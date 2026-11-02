package com.backend.finledger.security.dto;

import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.user.dto.UserDto;
import java.util.Set;

public record MeResponse(
    UserDto user,
    CompanyDto company,
    Set<String> permissions
) {}

