package com.flowpolicy.ia.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.ia.dto.AsistenciaIaRequest;
import com.flowpolicy.ia.dto.AsistenciaIaResponse;
import com.flowpolicy.ia.dto.CuelloBotellaResponse;
import com.flowpolicy.ia.dto.GenerarDiagramaRequest;
import com.flowpolicy.ia.dto.GenerarDiagramaResponse;
import com.flowpolicy.ia.dto.GenerarFormularioRequest;
import com.flowpolicy.ia.dto.GenerarFormularioResponse;
import com.flowpolicy.ia.service.AsistenciaIaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
public class AsistenciaIaController {

  private final AsistenciaIaService asistenciaIaService;

  /** Chat NLP: responde preguntas sobre el estado del sistema. */
  @PostMapping("/preguntar")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<AsistenciaIaResponse> preguntar(@Valid @RequestBody AsistenciaIaRequest request) {
    return ApiResponse.ok("Respuesta generada", asistenciaIaService.responder(request.pregunta()));
  }

  /** Genera un diagrama BPMN a partir de una descripción en lenguaje natural. */
  @PostMapping("/generar-diagrama")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<GenerarDiagramaResponse> generarDiagrama(@Valid @RequestBody GenerarDiagramaRequest request) {
    return ApiResponse.ok("Diagrama generado", asistenciaIaService.generarDiagramaDesdeTexto(request.descripcion()));
  }

  /** Genera campos de formulario a partir de la descripción de una tarea usando IA. */
  @PostMapping("/generar-formulario")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<GenerarFormularioResponse> generarFormulario(@Valid @RequestBody GenerarFormularioRequest request) {
    return ApiResponse.ok("Campos generados", asistenciaIaService.generarFormulario(request.descripcion(), request.nombreNodo()));
  }
}
