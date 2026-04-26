package com.flowpolicy.formulario.dto;

import java.util.List;

public record CampoResponse(
    String nombre,
    String etiqueta,
    String tipo,
    boolean requerido,
    List<String> opciones
) {
}
