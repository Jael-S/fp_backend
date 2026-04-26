package com.flowpolicy.usuario.dto;

public record UsuarioResponse(
    String id,
    String nombre,
    String email,
    String rol,
    String empresaId,
    String departamentoId,
    boolean activo
) {
}
