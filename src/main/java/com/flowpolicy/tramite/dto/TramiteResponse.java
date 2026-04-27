package com.flowpolicy.tramite.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record TramiteResponse(
    String id,
    String empresaId,
    String departamentoId,
    String politicaId,
    String titulo,
    String prioridad,
    LocalDateTime fechaLimite,
    String usuarioCreadorId,
    String creadoPorNombre,
    String estado,
    String nodoActualId,
    String departamentoActualId,
    String clienteNombre,
    String clienteIdentidad,
    String clienteEmail,
    String codigoSeguimiento,
    Map<String, Object> datos,
    LocalDateTime creadoEn,
    LocalDateTime actualizadoEn
) {
}
