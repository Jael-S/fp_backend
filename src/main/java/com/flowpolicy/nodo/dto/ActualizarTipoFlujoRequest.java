package com.flowpolicy.nodo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarTipoFlujoRequest {
  @NotBlank
  @JsonProperty("tipoFlujo")
  private String tipoFlujo; // SECUENCIAL, ALTERNATIVO, PARALELO, ITERATIVO

  /** Si se envía, el path `/{id}` se interpreta como elementId BPMN (ej. Gateway_xxx) */
  @JsonProperty("politicaId")
  private String politicaId;
}
