package com.flowpolicy.politica.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.politica.dto.PoliticaRequest;
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

@RestController
@RequestMapping("/api/v1/politicas")
@RequiredArgsConstructor
public class PoliticaController {

  private final PoliticaService politicaService;

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

  @Operation(summary = "Eliminar logico politica")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    politicaService.delete(id);
    return ApiResponse.ok("Politica eliminada", null);
  }
}
