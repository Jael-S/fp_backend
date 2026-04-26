package com.flowpolicy.departamento.service;

import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.departamento.dto.DepartamentoRequest;
import com.flowpolicy.departamento.dto.DepartamentoResponse;
import com.flowpolicy.departamento.model.Departamento;
import com.flowpolicy.departamento.repository.DepartamentoRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartamentoService {

  private final DepartamentoRepository departamentoRepository;
  private final UsuarioRepository usuarioRepository;
  private final CurrentUserService currentUserService;

  public PageResponse<DepartamentoResponse> list(int page, int size) {
    String empresaId = currentUserService.getEmpresaId();
    Page<Departamento> result = departamentoRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(page, size));
    return new PageResponse<>(
        result.map(this::toResponse).getContent(),
        result.getTotalElements(),
        result.getNumber(),
        result.getSize()
    );
  }

  public DepartamentoResponse create(DepartamentoRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    validateResponsable(empresaId, request.responsableId(), null);
    Departamento created = departamentoRepository.save(Departamento.builder()
        .empresaId(empresaId)
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .responsableId(request.responsableId())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    assignDepartamentoToResponsable(created.getId(), request.responsableId(), empresaId);
    log.info("Departamento creado id={} empresaId={}", created.getId(), empresaId);
    return toResponse(created);
  }

  public DepartamentoResponse update(String id, DepartamentoRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Departamento current = departamentoRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()))
        .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado"));

    String previousResponsableId = current.getResponsableId();
    validateResponsable(empresaId, request.responsableId(), current.getId());
    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setResponsableId(request.responsableId());

    Departamento updated = departamentoRepository.save(current);
    reassignResponsable(updated.getId(), previousResponsableId, request.responsableId(), empresaId);
    log.info("Departamento actualizado id={} empresaId={}", updated.getId(), empresaId);
    return toResponse(updated);
  }

  public void deactivate(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Departamento current = departamentoRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()))
        .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado"));
    clearDepartamentoFromUsers(id, empresaId);
    current.setResponsableId(null);
    current.setActivo(false);
    departamentoRepository.save(current);
    log.info("Departamento desactivado id={} empresaId={}", id, empresaId);
  }

  private void assignDepartamentoToResponsable(String departamentoId, String responsableId, String empresaId) {
    Usuario responsable = usuarioRepository.findById(responsableId)
        .filter(value -> empresaId.equals(value.getEmpresaId()))
        .orElseThrow(() -> new ResourceNotFoundException("Responsable no encontrado"));
    responsable.setDepartamentoId(departamentoId);
    usuarioRepository.save(responsable);
  }

  private void reassignResponsable(String departamentoId, String previousResponsableId, String newResponsableId, String empresaId) {
    if (previousResponsableId != null && !previousResponsableId.equals(newResponsableId)) {
      usuarioRepository.findById(previousResponsableId)
          .filter(value -> empresaId.equals(value.getEmpresaId()))
          .ifPresent(usuario -> {
            if (departamentoId.equals(usuario.getDepartamentoId())) {
              usuario.setDepartamentoId(null);
              usuarioRepository.save(usuario);
            }
          });
    }
    assignDepartamentoToResponsable(departamentoId, newResponsableId, empresaId);
  }

  private void clearDepartamentoFromUsers(String departamentoId, String empresaId) {
    List<Usuario> users = usuarioRepository.findByDepartamentoId(departamentoId);
    users.stream()
        .filter(u -> empresaId.equals(u.getEmpresaId()))
        .forEach(usuario -> {
          usuario.setDepartamentoId(null);
          usuarioRepository.save(usuario);
        });
  }

  private void validateResponsable(String empresaId, String responsableId, String currentDepartamentoId) {
    Usuario responsable = usuarioRepository.findById(responsableId)
        .filter(value -> empresaId.equals(value.getEmpresaId()))
        .orElseThrow(() -> new ResourceNotFoundException("Responsable no encontrado"));
    if (responsable.getRol() != Rol.ADMINISTRADOR_AREA) {
      throw new IllegalArgumentException("El responsable debe tener rol ADMINISTRADOR_AREA");
    }

    // Regla de negocio: un ADMINISTRADOR_AREA solo puede pertenecer a un departamento.
    // Si su departamentoId ya apunta a otro, bloqueamos la asignacion.
    String assignedDepartamentoId = responsable.getDepartamentoId();
    if (assignedDepartamentoId != null && !assignedDepartamentoId.isBlank()
        && (currentDepartamentoId == null || !assignedDepartamentoId.equals(currentDepartamentoId))) {
      String depName = departamentoRepository.findById(assignedDepartamentoId)
          .map(Departamento::getNombre)
          .orElse(assignedDepartamentoId);
      throw new IllegalArgumentException("Este usuario ya es Administrador del departamento " + depName);
    }

    // Validacion defensiva por inconsistencias historicas: no permitir multiples departamentos
    // activos con el mismo responsableId.
    departamentoRepository.findByEmpresaIdAndResponsableIdAndActivoTrue(empresaId, responsableId)
        .ifPresent(dep -> {
          if (currentDepartamentoId == null || !dep.getId().equals(currentDepartamentoId)) {
            throw new IllegalArgumentException("Este usuario ya es Administrador del departamento " + dep.getNombre());
          }
        });

    boolean alreadyAssigned = departamentoRepository.existsByEmpresaIdAndResponsableIdAndActivoTrue(
        empresaId, responsableId
    );
    if (alreadyAssigned && (currentDepartamentoId == null || !isCurrentResponsible(currentDepartamentoId, responsableId))) {
      throw new IllegalArgumentException("Este usuario ya es Administrador de otro departamento");
    }
  }

  private boolean isCurrentResponsible(String currentDepartamentoId, String responsableId) {
    return departamentoRepository.findById(currentDepartamentoId)
        .map(dep -> responsableId.equals(dep.getResponsableId()))
        .orElse(false);
  }

  private DepartamentoResponse toResponse(Departamento departamento) {
    long totalUsuarios = usuarioRepository.countByEmpresaIdAndDepartamentoIdAndActivoTrue(
        departamento.getEmpresaId(),
        departamento.getId()
    );
    return new DepartamentoResponse(
        departamento.getId(),
        departamento.getEmpresaId(),
        departamento.getNombre(),
        departamento.getDescripcion(),
        departamento.getResponsableId(),
        totalUsuarios,
        departamento.isActivo()
    );
  }
}
