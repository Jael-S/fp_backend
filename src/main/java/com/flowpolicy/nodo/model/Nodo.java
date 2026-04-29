package com.flowpolicy.nodo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

  @Field("elementId")
  private String elementId;

  @Field("formularioId")
  private String formularioId;

  @Field("departamentoId")
  private String departamentoId;

  @Field("carrilId")
  private String carrilId;

  @Field("carril")
  private String carril;

  @Field("condiciones")
  private List<Map<String, String>> condiciones;

  /** SECUENCIAL, ALTERNATIVO, PARALELO, ITERATIVO (gateways / decisión) */
  @Field("tipoFlujo")
  private String tipoFlujo;

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
