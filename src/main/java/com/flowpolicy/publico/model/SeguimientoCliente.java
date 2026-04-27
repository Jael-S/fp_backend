package com.flowpolicy.publico.model;

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
@Document(collection = "seguimiento_cliente")
public class SeguimientoCliente {
  @Id
  private String id;

  @Field("codigoSeguimiento")
  private String codigoSeguimiento;

  @Field("ipCliente")
  private String ipCliente;

  @Field("consultadoEn")
  private LocalDateTime consultadoEn;
}
