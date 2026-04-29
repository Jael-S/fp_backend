package com.flowpolicy.ejecucion.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EjecucionResponse(
    String id,
    String tramiteId,
    String tramiteTitulo,
    String nodoId,
    String nodoNombre,
    String departamentoId,
    String departamentoNombre,
    String usuarioAsignadoId,
    String estado,
    LocalDateTime inicioEjecucion,
    LocalDateTime finEjecucion,
    Long duracionMs,
    Map<String, Object> respuestasFormulario,
    List<String> adjuntos,
    String observaciones
) {}
