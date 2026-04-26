package com.flowpolicy.auth.model;

public enum Rol {
  GESTOR_SISTEMA,
  ADMINISTRADOR_AREA,
  FUNCIONARIO,
  /**
   * Compatibilidad con datos antiguos.
   * Se normaliza a FUNCIONARIO en servicios de negocio.
   */
  OPERADOR;

  public Rol normalized() {
    return this == OPERADOR ? FUNCIONARIO : this;
  }
}

