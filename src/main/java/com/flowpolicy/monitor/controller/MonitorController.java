package com.flowpolicy.monitor.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

  private final MonitorService monitorService;

  @GetMapping("/{politicaId}")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<Map<String, Object>> estado(@PathVariable String politicaId) {
    return ApiResponse.ok("Estado monitor obtenido", monitorService.estadoPolitica(politicaId));
  }
}
