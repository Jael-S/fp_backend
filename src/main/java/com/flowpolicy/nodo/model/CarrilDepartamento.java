package com.flowpolicy.nodo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Relación entre un Carril (Participant) de un diagrama BPMN
 * y un Departamento en el sistema.
 * 
 * Esto permite filtrar formularios por departamento cuando se edita una tarea.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "carriles_departamentos")
public class CarrilDepartamento {
  
  @Id
  private String id;

  @Field("politicaId")
  private String politicaId;

  @Field("empresaId")
  private String empresaId;

  @Field("carrilId")
  private String carrilId;

  @Field("carrilNombre")
  private String carrilNombre;

  @Field("departamentoId")
  private String departamentoId;

  @Field("departamentoNombre")
  private String departamentoNombre;

  @Field("creadoEn")
  private LocalDateTime creadoEn;

  @Field("actualizadoEn")
  private LocalDateTime actualizadoEn;

  @Field("activo")
  private Boolean activo;
}
