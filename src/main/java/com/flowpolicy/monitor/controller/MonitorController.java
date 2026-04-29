package com.flowpolicy.monitor.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.monitor.dto.MonitorResponseDTO;
import com.flowpolicy.monitor.dto.TramiteMonitorDTO;
import com.flowpolicy.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

  private final MonitorService monitorService;

  /** Estadísticas de todos los trámites de una política (vista general). */
  @GetMapping("/{politicaId}")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<MonitorResponseDTO> estado(@PathVariable String politicaId) {
    return ApiResponse.ok("Monitor obtenido", monitorService.estadoPolitica(politicaId));
  }

  /** Progreso de UN trámite específico: nodo actual, funcionario, historial, colores por nodo. */
  @GetMapping("/tramite/{tramiteId}")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA','FUNCIONARIO')")
  public ApiResponse<TramiteMonitorDTO> estadoTramite(@PathVariable String tramiteId) {
    return ApiResponse.ok("Monitor de tramite obtenido", monitorService.estadoTramite(tramiteId));
  }
}
