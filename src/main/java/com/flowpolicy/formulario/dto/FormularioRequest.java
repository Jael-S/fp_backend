package com.flowpolicy.formulario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FormularioRequest(
    @NotBlank String politicaId,
    @NotBlank String nodoId,
    @NotBlank String nombre,
    @Valid @NotEmpty List<CampoRequest> campos
) {
}
