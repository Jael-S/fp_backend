package com.flowpolicy.monitor.dto;

import java.util.List;
import java.util.Map;

public record MonitorResponseDTO(
    String politicaId,
    long total,
    long pendientes,
    long enProceso,
    long completados,
    long rechazados,
    Map<String, NodoMonitorInfo> nodos
) {

  /**
   * Info de un nodo del diagrama, indexada por su elementId BPMN (ej. "Task_1").
   * estado: SIN_TRAMITES | PENDIENTE | EN_PROCESO
   */
  public record NodoMonitorInfo(
      String nodoId,
      String elementId,
      String nombre,
      String tipo,
      int tramitesActivos,
      List<String> funcionarios,
      long tiempoPromedioMin,
      String estado
  ) {}
}
