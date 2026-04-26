package com.flowpolicy.tramite.model;

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
@Document(collection = "tramites")
public class Tramite {
  @Id
  private String id;

  @Field("empresaId")
  private String empresaId;

  @Field("departamentoId")
  private String departamentoId;

  @Field("politicaId")
  private String politicaId;

  @Field("usuarioCreadorId")
  private String usuarioCreadorId;

  @Field("estado")
  private EstadoTramite estado;

  @Field("nodoActualId")
  private String nodoActualId;

  @Field("datos")
  private Map<String, Object> datos;

  @Field("historial")
  private List<EventoTramite> historial;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;

  @Field("actualizadoEn")
  private LocalDateTime actualizadoEn;
}
