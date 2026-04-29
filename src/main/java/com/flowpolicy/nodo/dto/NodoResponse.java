package com.flowpolicy.nodo.dto;

public record NodoResponse(
    String id,
    String politicaId,
    String nombre,
    String descripcion,
    String tipo,
    String formularioId,
    Double posicionX,
    Double posicionY,
    String tipoFlujo
) {
}
