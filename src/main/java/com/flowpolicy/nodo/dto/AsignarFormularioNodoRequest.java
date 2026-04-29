package com.flowpolicy.nodo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarFormularioNodoRequest {
  
  @NotBlank(message = "nodoElementId es obligatorio")
  @JsonProperty("nodoElementId")
  private String nodoElementId;
  
  @JsonProperty("formularioId")
  private String formularioId;
}
