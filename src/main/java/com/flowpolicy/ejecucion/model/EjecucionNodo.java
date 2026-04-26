package com.flowpolicy.ejecucion.model;

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
@Document(collection = "ejecuciones_nodo")
public class EjecucionNodo {
  @Id
  private String id;

  @Field("empresaId")
  private String empresaId;

  @Field("departamentoId")
  private String departamentoId;

  @Field("tramiteId")
  private String tramiteId;

  @Field("nodoId")
  private String nodoId;

  @Field("usuarioAsignadoId")
  private String usuarioAsignadoId;

  @Field("estado")
  private EstadoEjecucion estado;

  @Field("inicioEjecucion")
  private LocalDateTime inicioEjecucion;

  @Field("finEjecucion")
  private LocalDateTime finEjecucion;

  @Field("duracionMs")
  private Long duracionMs;

  @Field("respuestasFormulario")
  private Map<String, Object> respuestasFormulario;

  @Field("adjuntos")
  private List<String> adjuntos;

  @Field("observaciones")
  private String observaciones;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
