package com.flowpolicy.formulario.dto;

import com.flowpolicy.formulario.model.CampoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CampoRequest(
    @NotBlank String nombre,
    @NotBlank String etiqueta,
    @NotNull CampoTipo tipo,
    boolean requerido,
    List<String> opciones
) {
}
