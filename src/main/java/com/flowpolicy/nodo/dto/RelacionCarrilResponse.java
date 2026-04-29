package com.flowpolicy.nodo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelacionCarrilResponse {
  
  @JsonProperty("carrilId")
  private String carrilId;
  
  @JsonProperty("carrilNombre")
  private String carrilNombre;
  
  @JsonProperty("departamentoId")
  private String departamentoId;
  
  @JsonProperty("departamentoNombre")
  private String departamentoNombre;
}
