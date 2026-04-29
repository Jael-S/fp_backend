package com.flowpolicy.nodo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuardarRelacionCarrilRequest {
  
  @NotBlank(message = "carrilId es obligatorio")
  @JsonProperty("carrilId")
  private String carrilId;
  
  @NotBlank(message = "carrilNombre es obligatorio")
  @JsonProperty("carrilNombre")
  private String carrilNombre;
  
  @NotBlank(message = "departamentoId es obligatorio")
  @JsonProperty("departamentoId")
  private String departamentoId;
  
  @NotBlank(message = "departamentoNombre es obligatorio")
  @JsonProperty("departamentoNombre")
  private String departamentoNombre;
}
