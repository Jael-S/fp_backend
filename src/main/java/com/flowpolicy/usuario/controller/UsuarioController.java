package com.flowpolicy.usuario.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.usuario.dto.UsuarioRequest;
import com.flowpolicy.usuario.dto.UsuarioResponse;
import com.flowpolicy.usuario.dto.UsuarioUpdateRequest;
import com.flowpolicy.usuario.service.UsuarioService;
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
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioService usuarioService;

  @Operation(summary = "Listar usuarios paginados por empresa")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @GetMapping
  public ApiResponse<PageResponse<UsuarioResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String departamentoId,
      @RequestParam(required = false) Rol rol,
      @RequestParam(required = false) String q
  ) {
    return ApiResponse.ok("Usuarios listados", usuarioService.list(page, size, departamentoId, rol, q));
  }

  @Operation(summary = "Obtener usuario por id")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  @GetMapping("/{id}")
  public ApiResponse<UsuarioResponse> getById(@PathVariable String id) {
    return ApiResponse.ok("Usuario encontrado", usuarioService.getById(id));
  }

  @Operation(summary = "Crear usuario")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA')")
  @PostMapping
  public ApiResponse<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest request) {
    return ApiResponse.ok("Usuario creado", usuarioService.create(request));
  }

  @Operation(summary = "Actualizar usuario")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA')")
  @PutMapping("/{id}")
  public ApiResponse<UsuarioResponse> update(@PathVariable String id, @Valid @RequestBody UsuarioUpdateRequest request) {
    return ApiResponse.ok("Usuario actualizado", usuarioService.update(id, request));
  }

  @Operation(summary = "Desactivar usuario")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> deactivate(@PathVariable String id) {
    usuarioService.deactivate(id);
    return ApiResponse.ok("Usuario desactivado", null);
  }
}
