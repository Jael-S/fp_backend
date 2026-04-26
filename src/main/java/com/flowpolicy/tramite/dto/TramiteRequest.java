package com.flowpolicy.tramite.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TramiteRequest(
    @NotBlank String politicaId,
    String departamentoId,
    Map<String, Object> datos
) {
}
