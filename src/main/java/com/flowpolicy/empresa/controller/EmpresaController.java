package com.flowpolicy.empresa.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.empresa.dto.EmpresaRequest;
import com.flowpolicy.empresa.dto.EmpresaResponse;
import com.flowpolicy.empresa.service.EmpresaService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
public class EmpresaController {

  private final EmpresaService empresaService;

  @Operation(summary = "Crear empresa")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping
  public ApiResponse<EmpresaResponse> create(@Valid @RequestBody EmpresaRequest request) {
    return ApiResponse.ok("Empresa creada", empresaService.create(request));
  }

  @Operation(summary = "Listar empresas activas")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @GetMapping
  public ApiResponse<List<EmpresaResponse>> list() {
    return ApiResponse.ok("Empresas listadas", empresaService.list());
  }

  @Operation(summary = "Obtener empresa por id")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @GetMapping("/{id}")
  public ApiResponse<EmpresaResponse> getById(@PathVariable String id) {
    return ApiResponse.ok("Empresa encontrada", empresaService.getById(id));
  }

  @Operation(summary = "Actualizar empresa")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}")
  public ApiResponse<EmpresaResponse> update(@PathVariable String id, @Valid @RequestBody EmpresaRequest request) {
    return ApiResponse.ok("Empresa actualizada", empresaService.update(id, request));
  }

  @Operation(summary = "Desactivar empresa")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> deactivate(@PathVariable String id) {
    empresaService.deactivate(id);
    return ApiResponse.ok("Empresa desactivada", null);
  }
}
