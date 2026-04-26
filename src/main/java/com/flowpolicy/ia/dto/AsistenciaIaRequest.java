package com.flowpolicy.ia.dto;

import jakarta.validation.constraints.NotBlank;

public record AsistenciaIaRequest(
    @NotBlank String pregunta
) {
}
