package com.flowpolicy.cobertura.dto;

import java.util.Map;

public record PuntoCoberturaResponse(
    String id,
    String empresaId,
    String departamentoId,
    String nombre,
    String tipo,
    double latitud,
    double longitud,
    Map<String, Object> metadata
) {
}
