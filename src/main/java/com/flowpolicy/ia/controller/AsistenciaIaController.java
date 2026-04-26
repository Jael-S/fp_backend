package com.flowpolicy.ia.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.ia.dto.AsistenciaIaRequest;
import com.flowpolicy.ia.dto.AsistenciaIaResponse;
import com.flowpolicy.ia.service.AsistenciaIaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
public class AsistenciaIaController {

  private final AsistenciaIaService asistenciaIaService;

  @PostMapping("/preguntar")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<AsistenciaIaResponse> preguntar(@Valid @RequestBody AsistenciaIaRequest request) {
    return ApiResponse.ok("Respuesta generada", asistenciaIaService.responder(request.pregunta()));
  }
}
