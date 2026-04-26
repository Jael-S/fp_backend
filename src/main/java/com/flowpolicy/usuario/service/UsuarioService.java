package com.flowpolicy.usuario.service;

import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.usuario.dto.UsuarioRequest;
import com.flowpolicy.usuario.dto.UsuarioResponse;
import com.flowpolicy.usuario.dto.UsuarioUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final CurrentUserService currentUserService;
  private final PasswordEncoder passwordEncoder;

  public PageResponse<UsuarioResponse> list(int page, int size, String departamentoId) {
    return list(page, size, departamentoId, null, null);
  }

  public PageResponse<UsuarioResponse> list(int page, int size, String departamentoId, Rol rol, String q) {
    String empresaId = currentUserService.getEmpresaId();
    Pageable pageable = PageRequest.of(page, size);

    Page<Usuario> result;
    if (q != null && !q.isBlank()) {
      result = usuarioRepository.findByEmpresaIdAndActivoTrueAndNombreContainingIgnoreCase(empresaId, q.trim(), pageable);
    } else if (departamentoId != null && !departamentoId.isBlank() && rol != null) {
      result = usuarioRepository.findByEmpresaIdAndDepartamentoIdAndRolAndActivoTrue(empresaId, departamentoId, rol.normalized(), pageable);
    } else if (departamentoId != null && !departamentoId.isBlank()) {
      result = usuarioRepository.findByEmpresaIdAndDepartamentoIdAndActivoTrue(empresaId, departamentoId, pageable);
    } else if (rol != null) {
      result = usuarioRepository.findByEmpresaIdAndRolAndActivoTrue(empresaId, rol.normalized(), pageable);
    } else {
      result = usuarioRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable);
    }

    return new PageResponse<>(
        result.map(this::toResponse).getContent(),
        result.getTotalElements(),
        result.getNumber(),
        result.getSize()
    );
  }

  public UsuarioResponse getById(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Usuario usuario = usuarioRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()))
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    return toResponse(usuario);
  }

  public UsuarioResponse create(UsuarioRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    if (usuarioRepository.existsByEmailAndEmpresaId(request.email(), empresaId)) {
      throw new DuplicateKeyException("El email ya se encuentra registrado en la empresa");
    }

    Usuario created = usuarioRepository.save(Usuario.builder()
        .nombre(request.nombre())
        .email(request.email())
        .passwordHash(passwordEncoder.encode(request.password()))
        .rol(request.rol().normalized())
        .empresaId(empresaId)
        .departamentoId(request.departamentoId())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    log.info("Usuario creado id={} empresaId={}", created.getId(), empresaId);
    return toResponse(created);
  }

  public UsuarioResponse update(String id, UsuarioUpdateRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Usuario usuario = usuarioRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()))
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

    if (!usuario.getEmail().equalsIgnoreCase(request.email())
        && usuarioRepository.existsByEmailAndEmpresaId(request.email(), empresaId)) {
      throw new DuplicateKeyException("El email ya se encuentra registrado en la empresa");
    }

    usuario.setNombre(request.nombre());
    usuario.setEmail(request.email());
    usuario.setRol(request.rol().normalized());
    usuario.setDepartamentoId(request.departamentoId());
    if (request.activo() != null) {
      usuario.setActivo(request.activo());
    }
    Usuario updated = usuarioRepository.save(usuario);
    log.info("Usuario actualizado id={} empresaId={}", updated.getId(), empresaId);
    return toResponse(updated);
  }

  public void deactivate(String id) {
    Usuario currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUserService.getEmpresaId();
    if (id.equals(currentUser.getId())) {
      throw new IllegalArgumentException("No puedes desactivar tu propio usuario");
    }
    Usuario usuario = usuarioRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()))
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    usuario.setActivo(false);
    usuarioRepository.save(usuario);
    log.info("Usuario desactivado id={} empresaId={}", id, empresaId);
  }

  private UsuarioResponse toResponse(Usuario usuario) {
    return new UsuarioResponse(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getEmail(),
        usuario.getRol().name(),
        usuario.getEmpresaId(),
        usuario.getDepartamentoId(),
        usuario.isActivo()
    );
  }
}
