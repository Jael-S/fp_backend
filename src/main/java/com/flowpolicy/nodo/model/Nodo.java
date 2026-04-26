package com.flowpolicy.nodo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "nodos")
public class Nodo {
  @Id
  private String id;

  @Field("politicaId")
  private String politicaId;

  @Field("empresaId")
  private String empresaId;

  @Field("nombre")
  private String nombre;

  @Field("descripcion")
  private String descripcion;

  @Field("tipo")
  private TipoNodo tipo;

  @Field("formularioId")
  private String formularioId;

  @Field("departamentoId")
  private String departamentoId;

  @Field("carril")
  private String carril;

  @Field("prioridad")
  private String prioridad;

  @Field("tiempoEstimado")
  private Integer tiempoEstimado;

  @Field("posicionX")
  private Double posicionX;

  @Field("posicionY")
  private Double posicionY;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
