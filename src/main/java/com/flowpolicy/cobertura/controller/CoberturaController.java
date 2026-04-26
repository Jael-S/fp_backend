package com.flowpolicy.cobertura.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.cobertura.dto.PuntoCoberturaRequest;
import com.flowpolicy.cobertura.dto.PuntoCoberturaResponse;
import com.flowpolicy.cobertura.service.CoberturaService;
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
@RequestMapping("/api/v1/cobertura")
@RequiredArgsConstructor
public class CoberturaController {

  private final CoberturaService coberturaService;

  @GetMapping
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<List<PuntoCoberturaResponse>> list() {
    return ApiResponse.ok("Puntos de cobertura listados", coberturaService.list());
  }

  @PostMapping
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<PuntoCoberturaResponse> create(@Valid @RequestBody PuntoCoberturaRequest request) {
    return ApiResponse.ok("Punto de cobertura creado", coberturaService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<PuntoCoberturaResponse> update(@PathVariable String id, @Valid @RequestBody PuntoCoberturaRequest request) {
    return ApiResponse.ok("Punto de cobertura actualizado", coberturaService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<Void> deactivate(@PathVariable String id) {
    coberturaService.deactivate(id);
    return ApiResponse.ok("Punto de cobertura eliminado", null);
  }
}
