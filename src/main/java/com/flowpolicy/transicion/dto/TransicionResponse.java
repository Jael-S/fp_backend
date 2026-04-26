package com.flowpolicy.transicion.dto;

public record TransicionResponse(
    String id,
    String politicaId,
    String nodoOrigenId,
    String nodoDestinoId,
    String nombre,
    String descripcion,
    String condicion,
    boolean requiereAprobacion
) {
}
