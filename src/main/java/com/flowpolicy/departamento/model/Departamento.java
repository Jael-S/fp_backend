package com.flowpolicy.departamento.model;

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
@Document(collection = "departamentos")
public class Departamento {
  @Id
  private String id;

  @Field("empresaId")
  private String empresaId;

  @Field("nombre")
  private String nombre;

  @Field("descripcion")
  private String descripcion;

  @Field("responsableId")
  private String responsableId;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
