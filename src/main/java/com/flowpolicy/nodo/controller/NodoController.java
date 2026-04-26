package com.flowpolicy.nodo.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.nodo.dto.NodoPosicionRequest;
import com.flowpolicy.nodo.dto.NodoRequest;
import com.flowpolicy.nodo.dto.NodoResponse;
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

@RestController
@RequestMapping("/api/v1/nodos")
@RequiredArgsConstructor
public class NodoController {
  private final NodoService nodoService;

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

  @Operation(summary = "Eliminar logico nodo")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    nodoService.delete(id);
    return ApiResponse.ok("Nodo eliminado", null);
  }
}
