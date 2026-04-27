package com.flowpolicy.tramite.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoTramite {
  private LocalDateTime fecha;
  private String nodoNombre;
  private String departamentoNombre;
  private Boolean completado;
  private String evento;
  private String usuarioId;
  private Map<String, Object> detalles;
}
