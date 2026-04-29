package com.flowpolicy.monitor.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TramiteMonitorDTO(
    String tramiteId,
    String titulo,
    String estado,
    String nodoActualNombre,
    String departamentoActualNombre,
    String funcionarioActualNombre,
    long tiempoTranscurridoMinutos,
    /**
     * Mapa elementId BPMN → estado del nodo para este trámite.
     * Valores: COMPLETADO | EN_PROCESO | PENDIENTE_FUTURO | SIN_NODO
     */
    Map<String, String> estadoNodos,
    List<EjecucionHistorialInfo> historialEjecuciones
) {
  public record EjecucionHistorialInfo(
      String nodoNombre,
      String estado,
      LocalDateTime inicio,
      LocalDateTime fin,
      long duracionMinutos
  ) {}
}
