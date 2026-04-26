package com.flowpolicy.auth.controller;

import com.flowpolicy.auth.dto.AuthResponse;
import com.flowpolicy.auth.dto.LoginRequest;
import com.flowpolicy.auth.dto.RegistroRequest;
import com.flowpolicy.auth.service.AuthService;
import com.flowpolicy.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "Autenticar usuario y obtener JWT")
  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("Solicitud login recibida para email={}", request.email());
    try {
      AuthResponse response = authService.login(request);
      log.info("Login exitoso para email={}, rol={}", response.email(), response.rol());
      return ApiResponse.ok("Login exitoso", response);
    } catch (Exception ex) {
      log.error("Error en login para email={}: {}", request.email(), ex.getMessage());
      throw ex;
    }
  }

  @Operation(summary = "Registrar empresa y administrador general")
  @PostMapping("/registro")
  public ApiResponse<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
    log.info("Solicitud registro recibida para empresa={}, email={}", request.nombreEmpresa(), request.email());
    try {
      AuthResponse response = authService.registro(request);
      log.info("Registro exitoso para empresaId={}, adminId={}", response.empresaId(), response.id());
      return ApiResponse.ok("Registro exitoso", response);
    } catch (Exception ex) {
      log.error("Error en registro para email={}: {}", request.email(), ex.getMessage());
      if (ex instanceof org.springframework.dao.DuplicateKeyException) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
      }
      throw ex;
    }
  }

  @Operation(summary = "Validar estado del servicio de autenticacion")
  @GetMapping("/health")
  public Map<String, String> health() {
    log.info("Solicitud health recibida");
    try {
      log.info("Health check exitoso");
      return Map.of("status", "OK");
    } catch (Exception ex) {
      log.error("Error en health check: {}", ex.getMessage());
      throw ex;
    }
  }
}

