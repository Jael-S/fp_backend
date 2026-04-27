package com.flowpolicy.formulario.dto;

import java.util.List;

public record FormularioResponse(
    String id,
    String politicaId,
    String nodoId,
    String nombre,
    String descripcion,
    String departamentoId,
    List<CampoResponse> campos
) {
}
