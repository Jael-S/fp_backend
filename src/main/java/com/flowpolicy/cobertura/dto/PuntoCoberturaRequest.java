package com.flowpolicy.cobertura.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PuntoCoberturaRequest(
    @NotBlank String nombre,
    @NotBlank String tipo,
    String departamentoId,
    @NotNull @Min(-90) @Max(90) Double latitud,
    @NotNull @Min(-180) @Max(180) Double longitud,
    Map<String, Object> metadata
) {
}
