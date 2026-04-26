package com.flowpolicy.usuario.dto;

import com.flowpolicy.auth.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
    @NotBlank String nombre,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotNull Rol rol,
    String departamentoId
) {
}
