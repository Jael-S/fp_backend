package com.flowpolicy.transicion.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.transicion.dto.TransicionRequest;
import com.flowpolicy.transicion.dto.TransicionResponse;
import com.flowpolicy.transicion.service.TransicionService;
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

@RestController
@RequestMapping("/api/v1/transiciones")
@RequiredArgsConstructor
public class TransicionController {

  private final TransicionService transicionService;

  @Operation(summary = "Crear transicion")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PostMapping
  public ApiResponse<TransicionResponse> create(
      @RequestParam String politicaId,
      @Valid @RequestBody TransicionRequest request
  ) {
    return ApiResponse.ok("Transicion creada", transicionService.create(politicaId, request));
  }

  @Operation(summary = "Listar transiciones por politica")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @GetMapping
  public ApiResponse<List<TransicionResponse>> listByPolitica(@RequestParam String politicaId) {
    return ApiResponse.ok("Transiciones listadas", transicionService.listByPolitica(politicaId));
  }

  @Operation(summary = "Actualizar transicion")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}")
  public ApiResponse<TransicionResponse> update(@PathVariable String id, @Valid @RequestBody TransicionRequest request) {
    return ApiResponse.ok("Transicion actualizada", transicionService.update(id, request));
  }

  @Operation(summary = "Eliminar logico transicion")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    transicionService.delete(id);
    return ApiResponse.ok("Transicion eliminada", null);
  }
}
