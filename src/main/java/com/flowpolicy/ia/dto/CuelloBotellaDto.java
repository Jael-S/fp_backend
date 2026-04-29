package com.flowpolicy.ia.dto;

import java.util.List;

/**
 * Resultado del análisis de cuellos de botella por nodo.
 * Campos alineados con el frontend (CuelloBotellaResponse en Angular).
 */
public record CuelloBotellaDto(
    List<NodoCuello> nodosCriticos,
    double tiempoPromedioGlobalMinutos,
    String analisisGeneral
) {
  public record NodoCuello(
      String nodoId,
      String nombre,
      double tiempoPromedioMinutos,
      int tramitesEjecutados,
      String nivelRiesgo,
      String sugerencia
  ) {}
}
