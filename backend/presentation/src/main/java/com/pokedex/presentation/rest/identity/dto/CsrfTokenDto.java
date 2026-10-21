package com.pokedex.presentation.rest.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** openapi {@code CsrfTokenDto}: the header to send and the token (the same value as the XSRF-TOKEN cookie). */
@Schema(name = "CsrfTokenDto")
public record CsrfTokenDto(String headerName, String token) {}
