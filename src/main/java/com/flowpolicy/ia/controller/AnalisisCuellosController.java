package com.flowpolicy.ia.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.ia.dto.CuelloBotellaDto;
import com.flowpolicy.ia.service.AnalisisCuellosService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
public class AnalisisCuellosController {

  private final AnalisisCuellosService analisisService;

  /**
   * Analiza cuellos de botella de una política usando datos reales de ejecución.
   * Considera ejecuciones COMPLETADO (tiempo histórico) y EN_PROCESO (tiempo actual).
   * Solo incluye nodos tipo PROCESO (tareas), excluye INICIO, DECISION y FIN.
   * Solo muestra nodos con tiempo promedio > 15 minutos.
   */
  @GetMapping("/analizar-cuellos/{politicaId}")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<CuelloBotellaDto> analizarCuellos(@PathVariable String politicaId) {
    return ApiResponse.ok("Análisis completado", analisisService.analizar(politicaId));
  }
}
