package com.flowpolicy.formulario.model;

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
@Document(collection = "formularios")
public class Formulario {
  @Id
  private String id;

  @Field("empresaId")
  private String empresaId;

  @Field("politicaId")
  private String politicaId;

  @Field("nodoId")
  private String nodoId;

  @Field("nombre")
  private String nombre;

  @Field("campos")
  private List<Campo> campos;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
