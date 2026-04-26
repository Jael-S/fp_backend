package com.flowpolicy.tramite.dto;

import com.flowpolicy.tramite.model.EventoTramite;

import java.util.List;

public record TramiteDetalleResponse(
    TramiteResponse tramite,
    List<EventoTramite> historial
) {
}
