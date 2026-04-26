package com.flowpolicy.notificacion.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
    String id,
    String tramiteId,
    String titulo,
    String mensaje,
    boolean leida,
    LocalDateTime fechaLectura,
    LocalDateTime creadoEn
) {
}
