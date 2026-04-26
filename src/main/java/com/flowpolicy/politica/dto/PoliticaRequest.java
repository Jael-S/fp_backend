package com.flowpolicy.politica.dto;

import jakarta.validation.constraints.NotBlank;

public record PoliticaRequest(
    @NotBlank String nombre,
    String descripcion,
    String diagramaJson
) {
}
