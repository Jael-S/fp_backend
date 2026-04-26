package com.flowpolicy.politica.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "politicas")
public class Politica {
  @Id
  private String id;

  @Field("empresaId")
  private String empresaId;

  @Field("nombre")
  private String nombre;

  @Field("descripcion")
  private String descripcion;

  @Field("version")
  private int version;

  @Field("estado")
  private EstadoPolitica estado;

  @Field("creadoPor")
  private String creadoPor;

  @Field("diagramaJson")
  private String diagramaJson;

  @Field("nodoIds")
  private List<String> nodoIds;

  @Field("transicionIds")
  private List<String> transicionIds;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
