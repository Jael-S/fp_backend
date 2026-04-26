package com.flowpolicy.tramite.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record TramiteResponse(
    String id,
    String empresaId,
    String departamentoId,
    String politicaId,
    String usuarioCreadorId,
    String estado,
    String nodoActualId,
    Map<String, Object> datos,
    LocalDateTime creadoEn,
    LocalDateTime actualizadoEn
) {
}
