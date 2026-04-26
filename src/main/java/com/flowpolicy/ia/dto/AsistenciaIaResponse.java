package com.flowpolicy.ia.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AsistenciaIaResponse(
    String respuesta,
    Map<String, Object> metricas,
    LocalDateTime generadoEn
) {
}
