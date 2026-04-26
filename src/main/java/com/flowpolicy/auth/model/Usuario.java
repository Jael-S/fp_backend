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
@Document(collection = "usuarios")
public class Usuario {
  @Id
  private String id;

  @Field("nombre")
  private String nombre;

  @Field("email")
  private String email;

  @Field("passwordHash")
  private String passwordHash;

  @Field("rol")
  private Rol rol;

  @Field("empresaId")
  private String empresaId;

  @Field("departamentoId")
  private String departamentoId;

  @Field("activo")
  private boolean activo;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}

