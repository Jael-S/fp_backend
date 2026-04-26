package com.flowpolicy.empresa.dto;

public record EmpresaResponse(
    String id,
    String nombre,
    String descripcion,
    String email,
    String telefono,
    boolean activo
) {
}
