package com.flowpolicy.ejecucion.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.ejecucion.dto.EjecucionResponse;
import com.flowpolicy.ejecucion.dto.RechazoRequest;
import com.flowpolicy.ejecucion.service.EjecucionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ejecuciones")
@RequiredArgsConstructor
public class EjecucionController {

  private final EjecucionService ejecucionService;

  @GetMapping("/pendientes")
  public ApiResponse<List<EjecucionResponse>> pendientes() {
    return ApiResponse.ok("Ejecuciones pendientes", ejecucionService.pendientes());
  }

  @GetMapping("/mis-tareas")
  public ApiResponse<List<EjecucionResponse>> misTareas() {
    return ApiResponse.ok("Mis tareas pendientes", ejecucionService.pendientes());
  }

  @GetMapping("/historial")
  public ApiResponse<List<EjecucionResponse>> historial() {
    return ApiResponse.ok("Historial de tareas", ejecucionService.historial());
  }

  @GetMapping
  public ApiResponse<List<EjecucionResponse>> byTramite(@RequestParam String tramiteId) {
    return ApiResponse.ok("Ejecuciones por tramite", ejecucionService.byTramite(tramiteId));
  }

  @GetMapping("/{id}")
  public ApiResponse<EjecucionResponse> getById(@PathVariable String id) {
    return ApiResponse.ok("Ejecucion encontrada", ejecucionService.getById(id));
  }

  @PutMapping("/{id}/iniciar")
  public ApiResponse<EjecucionResponse> iniciar(@PathVariable String id) {
    return ApiResponse.ok("Ejecucion iniciada", ejecucionService.iniciar(id));
  }

  @PutMapping(value = "/{id}/completar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<EjecucionResponse> completar(
      @PathVariable String id,
      @RequestPart(value = "respuestas", required = false) String respuestas,
      @RequestPart(value = "archivos", required = false) MultipartFile[] archivos
  ) {
    return ApiResponse.ok("Ejecucion completada", ejecucionService.completar(id, respuestas, archivos));
  }

  @PutMapping("/{id}/rechazar")
  public ApiResponse<EjecucionResponse> rechazar(@PathVariable String id, @Valid @RequestBody RechazoRequest request) {
    return ApiResponse.ok("Ejecucion rechazada", ejecucionService.rechazar(id, request.observaciones()));
  }
}
