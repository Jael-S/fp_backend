package com.flowpolicy.ia.dto;

import java.util.List;

public record CuelloBotellaResponse(
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
