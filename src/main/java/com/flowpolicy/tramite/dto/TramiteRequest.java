package com.flowpolicy.tramite.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Map;

public record TramiteRequest(
    @NotBlank String politicaId,
    @NotBlank String titulo,
    String prioridad,
    LocalDateTime fechaLimite,
    String clienteNombre,
    String clienteIdentidad,
    String clienteEmail,
    String codigoSeguimiento,
    String departamentoId,
    Map<String, Object> datos
) {
}
