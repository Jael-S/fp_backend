package com.flowpolicy.nodo.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.nodo.dto.ActualizarTipoFlujoRequest;
import com.flowpolicy.nodo.dto.AsignarFormularioRequest;
import com.flowpolicy.nodo.dto.GuardarRelacionCarrilRequest;
import com.flowpolicy.nodo.dto.NodoPosicionRequest;
import com.flowpolicy.nodo.dto.NodoRequest;
import com.flowpolicy.nodo.dto.NodoPorElementoResponse;
import com.flowpolicy.nodo.dto.NodoResponse;
import com.flowpolicy.nodo.dto.RelacionCarrilResponse;
import com.flowpolicy.nodo.service.CarrilService;
import com.flowpolicy.nodo.service.NodoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/nodos")
@RequiredArgsConstructor
public class NodoController {
  private final NodoService nodoService;
  private final CarrilService carrilService;

  @Operation(summary = "Crear nodo")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping
  public ApiResponse<NodoResponse> create(@RequestParam String politicaId, @Valid @RequestBody NodoRequest request) {
    return ApiResponse.ok("Nodo creado", nodoService.create(politicaId, request));
  }

  @Operation(summary = "Listar nodos por politica")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @GetMapping
  public ApiResponse<List<NodoResponse>> listByPolitica(@RequestParam String politicaId) {
    return ApiResponse.ok("Nodos listados", nodoService.listByPolitica(politicaId));
  }

  @Operation(summary = "Actualizar nodo")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}")
  public ApiResponse<NodoResponse> update(@PathVariable String id, @Valid @RequestBody NodoRequest request) {
    return ApiResponse.ok("Nodo actualizado", nodoService.update(id, request));
  }

  @Operation(summary = "Actualizar posicion de nodo")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}/posicion")
  public ApiResponse<NodoResponse> updatePosition(@PathVariable String id, @Valid @RequestBody NodoPosicionRequest request) {
    return ApiResponse.ok("Posicion de nodo actualizada", nodoService.updatePosition(id, request));
  }

  @Operation(summary = "Asignar formulario a nodo/tarea")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}/formulario")
  public ApiResponse<NodoResponse> asignarFormulario(@PathVariable String id, @Valid @RequestBody AsignarFormularioRequest request) {
    return ApiResponse.ok("Formulario asignado a nodo", nodoService.asignarFormulario(id, request.getFormularioId()));
  }

  @Operation(summary = "Actualizar tipo de flujo de decisión (soporta elementId BPMN si se envía politicaId)")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}/tipo-flujo")
  public ApiResponse<NodoResponse> actualizarTipoFlujo(@PathVariable String id, @Valid @RequestBody ActualizarTipoFlujoRequest request) {
    return ApiResponse.ok("Tipo de flujo actualizado",
        nodoService.actualizarTipoFlujo(id, request.getTipoFlujo(), request.getPoliticaId()));
  }

  @Operation(summary = "Obtener configuración de nodo por elementId BPMN")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @GetMapping("/elemento/{elementId}/politica/{politicaId}")
  public ApiResponse<NodoPorElementoResponse> findByElemento(
      @PathVariable String elementId,
      @PathVariable String politicaId) {
    Optional<NodoPorElementoResponse> result = nodoService.findByElementIdAndPoliticaId(elementId, politicaId);
    return ApiResponse.ok("Nodo encontrado", result.orElse(null));
  }

  @Operation(summary = "Configurar tarea BPMN por elementId: nombre, departamento y formulario")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{elementId}/config")
  public ApiResponse<NodoResponse> configurarPorElementId(
      @PathVariable String elementId,
      @RequestBody Map<String, String> body) {
    return ApiResponse.ok("Tarea configurada", nodoService.configurarPorElementId(elementId, body));
  }

  @Operation(summary = "Eliminar logico nodo")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    nodoService.delete(id);
    return ApiResponse.ok("Nodo eliminado", null);
  }

  // ===== ENDPOINTS PARA RELACIÓN CARRIL-DEPARTAMENTO =====
  
  @Operation(summary = "Guardar relación carril-departamento")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping("/carriles/{politicaId}/relacion")
  public ApiResponse<RelacionCarrilResponse> guardarRelacionCarril(
      @PathVariable String politicaId,
      @Valid @RequestBody GuardarRelacionCarrilRequest request) {
    return ApiResponse.ok("Relación carril-departamento guardada", 
        carrilService.guardarRelacion(politicaId, request));
  }

  @Operation(summary = "Obtener relación carril-departamento")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @GetMapping("/carriles/{politicaId}/relacion/{carrilId}")
  public ApiResponse<RelacionCarrilResponse> obtenerRelacionCarril(
      @PathVariable String politicaId,
      @PathVariable String carrilId) {
    return ApiResponse.ok("Relación obtenida", 
        carrilService.obtenerRelacionPorPolitica(politicaId, carrilId));
  }

  @Operation(summary = "Asignar formulario a tarea (por elementId del nodo)")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping("/tareas/{politicaId}/{nodoElementId}/formulario")
  public ApiResponse<String> asignarFormularioATarea(
      @PathVariable String politicaId,
      @PathVariable String nodoElementId,
      @RequestParam String formularioId) {
    nodoService.asignarFormularioATarea(politicaId, nodoElementId, formularioId);
    return ApiResponse.ok("Formulario asignado a tarea", "OK");
  }
}
