package com.flowpolicy.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(
    @NotBlank String nombreEmpresa,
    @NotBlank String nombreAdmin,
    @NotBlank @Email String email,
    @NotBlank String password
) {
}
