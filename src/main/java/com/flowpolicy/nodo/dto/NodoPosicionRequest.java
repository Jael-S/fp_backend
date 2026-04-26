package com.flowpolicy.nodo.dto;

import jakarta.validation.constraints.NotNull;

public record NodoPosicionRequest(
    @NotNull Double posicionX,
    @NotNull Double posicionY
) {
}
