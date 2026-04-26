package com.flowpolicy.ejecucion.dto;

import jakarta.validation.constraints.NotBlank;

public record RechazoRequest(
    @NotBlank String observaciones
) {
}
