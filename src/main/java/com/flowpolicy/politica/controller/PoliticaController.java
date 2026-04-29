package com.flowpolicy.politica.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.nodo.service.NodoService;
import com.flowpolicy.politica.dto.PoliticaRequest;
import com.flowpolicy.transicion.dto.TransicionResponse;
import com.flowpolicy.transicion.service.TransicionService;
import com.flowpolicy.politica.dto.PoliticaResponse;
import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.service.PoliticaService;
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

@RestController
@RequestMapping("/api/v1/politicas")
@RequiredArgsConstructor
public class PoliticaController {

  private final PoliticaService politicaService;
  private final NodoService nodoService;
  private final TransicionService transicionService;

  @Operation(summary = "Crear politica BORRADOR version 1")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping
  public ApiResponse<PoliticaResponse> create(@Valid @RequestBody PoliticaRequest request) {
    return ApiResponse.ok("Politica creada", politicaService.create(request));
  }

  @Operation(summary = "Actualizar politica y versionar si esta activa")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}")
  public ApiResponse<PoliticaResponse> update(@PathVariable String id, @Valid @RequestBody PoliticaRequest request) {
    return ApiResponse.ok("Politica actualizada", politicaService.update(id, request));
  }

  @Operation(summary = "Activar politica")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping("/{id}/activar")
  public ApiResponse<PoliticaResponse> activate(@PathVariable String id) {
    return ApiResponse.ok("Politica activada", politicaService.activate(id));
  }

  @Operation(summary = "Desactivar politica")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping("/{id}/desactivar")
  public ApiResponse<PoliticaResponse> deactivate(@PathVariable String id) {
    return ApiResponse.ok("Politica desactivada", politicaService.deactivate(id));
  }

  @Operation(summary = "Obtener politica por id")
  @GetMapping("/{id}")
  public ApiResponse<PoliticaResponse> getById(@PathVariable String id) {
    return ApiResponse.ok("Politica encontrada", politicaService.getById(id));
  }

  @Operation(summary = "Listar politicas con filtro opcional por estado")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA','FUNCIONARIO')")
  @GetMapping
  public ApiResponse<PageResponse<PoliticaResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) EstadoPolitica estado
  ) {
    return ApiResponse.ok("Politicas listadas", politicaService.list(page, size, estado));
  }

  @Operation(summary = "Listar politicas activas")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @GetMapping("/activas")
  public ApiResponse<java.util.List<PoliticaResponse>> listActivas() {
    return ApiResponse.ok("Politicas activas listadas", politicaService.listActivas());
  }

  @Operation(summary = "Eliminar logico politica")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    politicaService.delete(id);
    return ApiResponse.ok("Politica eliminada", null);
  }

  @Operation(summary = "Guardar diagrama + nodos + transiciones")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}/diagrama")
  public ApiResponse<Map<String, Object>> saveDiagrama(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    return ApiResponse.ok("Diagrama guardado", politicaService.saveDiagrama(id, payload));
  }

  @Operation(summary = "Obtener diagrama + nodos + transiciones")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA','FUNCIONARIO')")
  @GetMapping("/{id}/diagrama")
  public ApiResponse<Map<String, Object>> getDiagrama(@PathVariable String id) {
    return ApiResponse.ok("Diagrama obtenido", politicaService.getDiagrama(id));
  }

  @Operation(summary = "Guardar XML + nodos + transiciones + relaciones")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}/diagrama/completo")
  public ApiResponse<Map<String, Object>> guardarDiagramaCompleto(
      @PathVariable String id,
      @RequestBody Map<String, Object> payload
  ) {
    String xml = String.valueOf(payload.getOrDefault("xml", payload.getOrDefault("diagramaXml", "")));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> transiciones = (List<Map<String, Object>>) payload.get("transiciones");
    return ApiResponse.ok("Diagrama completo guardado", politicaService.guardarDiagramaConRelaciones(id, xml, transiciones));
  }

  @Operation(summary = "Listar transiciones persistidas de la política")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @GetMapping("/{id}/transiciones")
  public ApiResponse<List<TransicionResponse>> listTransiciones(@PathVariable String id) {
    return ApiResponse.ok("Transiciones listadas", transicionService.listByPolitica(id));
  }

  @Operation(summary = "Asignar formulario a tarea")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping("/{id}/tareas/{taskId}/formulario")
  public ApiResponse<Void> asignarFormularioATarea(
      @PathVariable String id,
      @PathVariable String taskId,
      @RequestBody Map<String, String> payload
  ) {
    politicaService.asignarFormularioATarea(id, taskId, payload.get("formularioId"));
    return ApiResponse.ok("Formulario asignado a tarea", null);
  }

  @Operation(summary = "Guardar condiciones de una decision")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}/decisiones/{gatewayId}/condiciones")
  public ApiResponse<Void> guardarCondicionesDecision(
      @PathVariable String id,
      @PathVariable String gatewayId,
      @RequestBody Map<String, Object> payload
  ) {
    politicaService.guardarCondicionesDecision(id, gatewayId, payload);
    return ApiResponse.ok("Condiciones guardadas", null);
  }

  @Operation(summary = "Obtener XML + nodos + transiciones + relaciones")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA','FUNCIONARIO')")
  @GetMapping("/{id}/diagrama/completo")
  public ApiResponse<Map<String, Object>> obtenerDiagramaCompleto(@PathVariable String id) {
    return ApiResponse.ok("Diagrama completo obtenido", politicaService.obtenerDiagramaCompleto(id));
  }

  @Operation(summary = "Obtener formulario asociado a un nodo")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA','FUNCIONARIO')")
  @GetMapping("/nodos/{id}/formulario")
  public ApiResponse<Map<String, String>> getFormularioByNodo(@PathVariable String id) {
    return ApiResponse.ok("Formulario de nodo obtenido", nodoService.getFormularioByNodoId(id));
  }
}
