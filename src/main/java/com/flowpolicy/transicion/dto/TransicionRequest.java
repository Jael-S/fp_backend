package com.flowpolicy.transicion.dto;

import jakarta.validation.constraints.NotBlank;

public record TransicionRequest(
    @NotBlank String nodoOrigenId,
    @NotBlank String nodoDestinoId,
    String nombre,
    String descripcion,
    String condicion,
    Boolean requiereAprobacion,
    String tipo,
    String etiqueta
) {
}
