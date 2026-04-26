package com.flowpolicy.departamento.dto;

public record DepartamentoResponse(
    String id,
    String empresaId,
    String nombre,
    String descripcion,
    String responsableId,
    long cantidadUsuarios,
    boolean activo
) {
}
