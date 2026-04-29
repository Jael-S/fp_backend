package com.flowpolicy.ia.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerarDiagramaRequest(
    @NotBlank String descripcion,
    String politicaId
) {}
