package com.flowpolicy.formulario.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campo {
  private String nombre;
  private String etiqueta;
  private CampoTipo tipo;
  private boolean requerido;
  private List<String> opciones;
}
