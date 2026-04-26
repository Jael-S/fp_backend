package com.flowpolicy.politica.dto;

public record PoliticaResponse(
    String id,
    String empresaId,
    String nombre,
    String descripcion,
    int version,
    String estado,
    String creadoPor,
    String diagramaJson
) {
}
