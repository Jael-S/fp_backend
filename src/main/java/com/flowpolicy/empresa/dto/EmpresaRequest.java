package com.flowpolicy.empresa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmpresaRequest(
    @NotBlank String nombre,
    String descripcion,
    @NotBlank @Email String email,
    String telefono
) {
}
