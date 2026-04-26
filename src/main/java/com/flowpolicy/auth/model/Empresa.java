package com.flowpolicy.auth.model;

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
@Document(collection = "empresas")
public class Empresa {
  @Id
  private String id;

  @Field("nombre")
  private String nombre;

  @Field("descripcion")
  private String descripcion;

  @Field("email")
  private String email;

  @Field("telefono")
  private String telefono;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
