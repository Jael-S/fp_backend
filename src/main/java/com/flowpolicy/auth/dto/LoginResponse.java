package com.flowpolicy.auth.dto;

public record LoginResponse(
    String token,
    String rol,
    String nombre,
    String email
) {
}

