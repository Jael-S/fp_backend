package com.flowpolicy.formulario.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.formulario.dto.FormularioRequest;
import com.flowpolicy.formulario.dto.FormularioResponse;
import com.flowpolicy.formulario.service.FormularioService;
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
@RequestMapping("/api/v1/formularios")
@RequiredArgsConstructor
public class FormularioController {

  private final FormularioService formularioService;

  @Operation(summary = "Crear formulario dinamico")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @PostMapping
  public ApiResponse<FormularioResponse> create(@Valid @RequestBody FormularioRequest request) {
    return ApiResponse.ok("Formulario creado", formularioService.create(request));
  }

  @Operation(summary = "Buscar formularios por nodo")
  @GetMapping("/nodo/{nodoId}")
  public ApiResponse<List<FormularioResponse>> byNodo(@PathVariable String nodoId) {
    return ApiResponse.ok("Formularios listados", formularioService.findByNodoId(nodoId));
  }

  @Operation(summary = "Actualizar formulario")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @PutMapping("/{id}")
  public ApiResponse<FormularioResponse> update(@PathVariable String id, @Valid @RequestBody FormularioRequest request) {
    return ApiResponse.ok("Formulario actualizado", formularioService.update(id, request));
  }

  @Operation(summary = "Eliminar logico formulario")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> deactivate(@PathVariable String id) {
    formularioService.deactivate(id);
    return ApiResponse.ok("Formulario eliminado", null);
  }
}
