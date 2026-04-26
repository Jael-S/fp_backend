package com.flowpolicy.auth.dto;

public record AuthResponse(
    String token,
    String id,
    String nombre,
    String email,
    String rol,
    String empresaId,
    String departamentoId
) {
}
