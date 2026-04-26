package com.flowpolicy.departamento.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.departamento.dto.DepartamentoRequest;
import com.flowpolicy.departamento.dto.DepartamentoResponse;
import com.flowpolicy.departamento.service.DepartamentoService;
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
@RequestMapping("/api/v1/departamentos")
@RequiredArgsConstructor
public class DepartamentoController {

  private final DepartamentoService departamentoService;

  @Operation(summary = "Listar departamentos paginados")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @GetMapping
  public ApiResponse<PageResponse<DepartamentoResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ApiResponse.ok("Departamentos listados", departamentoService.list(page, size));
  }

  @Operation(summary = "Crear departamento")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping
  public ApiResponse<DepartamentoResponse> create(@Valid @RequestBody DepartamentoRequest request) {
    return ApiResponse.ok("Departamento creado", departamentoService.create(request));
  }

  @Operation(summary = "Actualizar departamento")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}")
  public ApiResponse<DepartamentoResponse> update(@PathVariable String id, @Valid @RequestBody DepartamentoRequest request) {
    return ApiResponse.ok("Departamento actualizado", departamentoService.update(id, request));
  }

  @Operation(summary = "Desactivar departamento")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> deactivate(@PathVariable String id) {
    departamentoService.deactivate(id);
    return ApiResponse.ok("Departamento desactivado", null);
  }
}
