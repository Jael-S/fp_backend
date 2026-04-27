package com.flowpolicy.notificacion.model;

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
@Document(collection = "notificaciones")
public class Notificacion {
  @Id
  private String id;

  @Field("empresaId")
  private String empresaId;

  @Field("usuarioId")
  private String usuarioId;

  @Field("tramiteId")
  private String tramiteId;

  @Field("ejecucionId")
  private String ejecucionId;

  @Field("tipo")
  private String tipo;

  @Field("titulo")
  private String titulo;

  @Field("mensaje")
  private String mensaje;

  @Field("leida")
  private boolean leida;

  @Field("fechaLectura")
  private LocalDateTime fechaLectura;

  @Field("creadoEn")
  private LocalDateTime creadoEn;
}
