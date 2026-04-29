package com.flowpolicy.ia.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerarFormularioRequest(
    @NotBlank String descripcion,
    String nombreNodo
) {}
