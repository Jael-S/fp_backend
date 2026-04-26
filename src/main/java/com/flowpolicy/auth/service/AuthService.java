package com.flowpolicy.auth.service;

import com.flowpolicy.auth.dto.AuthResponse;
import com.flowpolicy.auth.dto.LoginRequest;
import com.flowpolicy.auth.dto.RegistroRequest;
import com.flowpolicy.auth.model.Empresa;
import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.EmpresaRepository;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.security.JwtUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

  private final UsuarioRepository usuarioRepository;
  private final EmpresaRepository empresaRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public AuthService(
      UsuarioRepository usuarioRepository,
      EmpresaRepository empresaRepository,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil
  ) {
    this.usuarioRepository = usuarioRepository;
    this.empresaRepository = empresaRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  public AuthResponse login(LoginRequest request) {
    Usuario usuario = usuarioRepository.findByEmail(request.email())
        .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

    if (!usuario.isActivo()) {
      throw new BadCredentialsException("Usuario inactivo");
    }

    if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
      throw new BadCredentialsException("Credenciales incorrectas");
    }

    var userDetails = org.springframework.security.core.userdetails.User.builder()
        .username(usuario.getEmail())
        .password(usuario.getPasswordHash())
        .authorities("ROLE_" + usuario.getRol().name())
        .build();

    String token = jwtUtil.generateToken(userDetails, usuario.getRol().name(), usuario.getEmpresaId());

    return buildAuthResponse(usuario, token);
  }

  public AuthResponse registro(RegistroRequest request) {
    if (usuarioRepository.existsByEmail(request.email())) {
      throw new DuplicateKeyException("El email ya se encuentra registrado");
    }

    Empresa empresa = empresaRepository.save(
        Empresa.builder()
            .nombre(request.nombreEmpresa())
            .activo(true)
            .creadoEn(LocalDateTime.now())
            .build()
    );

    Usuario adminGeneral = usuarioRepository.save(
        Usuario.builder()
            .nombre(request.nombreAdmin())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .rol(Rol.GESTOR_SISTEMA)
            .empresaId(empresa.getId())
            .departamentoId(null)
            .activo(true)
            .creadoEn(LocalDateTime.now())
            .build()
    );

    var userDetails = org.springframework.security.core.userdetails.User.builder()
        .username(adminGeneral.getEmail())
        .password(adminGeneral.getPasswordHash())
        .authorities("ROLE_" + adminGeneral.getRol().name())
        .build();

    String token = jwtUtil.generateToken(userDetails, adminGeneral.getRol().name(), adminGeneral.getEmpresaId());
    return buildAuthResponse(adminGeneral, token);
  }

  private AuthResponse buildAuthResponse(Usuario usuario, String token) {
    return new AuthResponse(
        token,
        usuario.getId(),
        usuario.getNombre(),
        usuario.getEmail(),
        usuario.getRol().name(),
        usuario.getEmpresaId(),
        usuario.getDepartamentoId()
    );
  }
}

