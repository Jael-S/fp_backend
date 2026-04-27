package com.flowpolicy.formulario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FormularioRequest(
    String politicaId,
    String nodoId,
    @NotBlank String nombre,
    String descripcion,
    String departamentoId,
    @Valid @NotEmpty List<CampoRequest> campos
) {
}
