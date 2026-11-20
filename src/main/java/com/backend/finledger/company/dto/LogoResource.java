package com.backend.finledger.company.dto;

import org.springframework.core.io.Resource;

public record LogoResource(
    Resource resource,
    String contentType,
    String filename
) {}
