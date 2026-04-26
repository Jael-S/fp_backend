package com.flowpolicy.nodo.dto;

import com.flowpolicy.nodo.model.TipoNodo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NodoRequest(
    @NotBlank String nombre,
    String descripcion,
    @NotNull TipoNodo tipo,
    String formularioId,
    Double posicionX,
    Double posicionY
) {
}
