package com.flowpolicy.ia.dto;

import java.util.List;

public record GenerarFormularioResponse(List<CampoIa> campos) {

  public record CampoIa(
      String nombre,
      String etiqueta,
      String tipo,
      boolean requerido,
      boolean esCampoPrioridad,
      List<String> opciones
  ) {}
}
