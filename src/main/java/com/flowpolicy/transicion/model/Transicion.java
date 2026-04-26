package com.flowpolicy.transicion.model;

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
@Document(collection = "transiciones")
public class Transicion {
  @Id
  private String id;

  @Field("politicaId")
  private String politicaId;

  @Field("empresaId")
  private String empresaId;

  @Field("nodoOrigenId")
  private String nodoOrigenId;

  @Field("nodoDestinoId")
  private String nodoDestinoId;

  @Field("nombre")
  private String nombre;

  @Field("descripcion")
  private String descripcion;

  @Field("condicion")
  private String condicion;

  @Field("requiereAprobacion")
  private boolean requiereAprobacion;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
