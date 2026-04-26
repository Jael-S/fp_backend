package com.flowpolicy.departamento.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartamentoRequest(
    @NotBlank String nombre,
    String descripcion,
    @NotBlank String responsableId
) {
}
