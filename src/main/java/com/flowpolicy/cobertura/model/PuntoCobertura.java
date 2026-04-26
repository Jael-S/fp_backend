package com.flowpolicy.cobertura.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "puntos_cobertura")
public class PuntoCobertura {
  @Id
  private String id;

  @Field("empresaId")
  private String empresaId;

  @Field("departamentoId")
  private String departamentoId;

  @Field("nombre")
  private String nombre;

  @Field("tipo")
  private String tipo;

  @Field("latitud")
  private double latitud;

  @Field("longitud")
  private double longitud;

  @Field("metadata")
  private Map<String, Object> metadata;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
