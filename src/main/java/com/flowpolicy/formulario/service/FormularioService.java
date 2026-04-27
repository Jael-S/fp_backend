package com.flowpolicy.formulario.service;

import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.formulario.dto.CampoResponse;
import com.flowpolicy.formulario.dto.CampoRequest;
import com.flowpolicy.formulario.dto.FormularioRequest;
import com.flowpolicy.formulario.dto.FormularioResponse;
import com.flowpolicy.formulario.model.Campo;
import com.flowpolicy.formulario.model.Formulario;
import com.flowpolicy.formulario.repository.FormularioRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormularioService {

  private final FormularioRepository formularioRepository;
  private final CurrentUserService currentUserService;

  public FormularioResponse create(FormularioRequest request) {
    var currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    String departamentoId = resolveDepartamentoId(request.departamentoId(), currentUser.getDepartamentoId(), currentUser.getRol());
    Formulario created = formularioRepository.save(Formulario.builder()
        .empresaId(empresaId)
        .politicaId(request.politicaId())
        .nodoId(request.nodoId())
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .departamentoId(departamentoId)
        .creadoPor(currentUser.getId())
        .campos(mapCampos(request))
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    log.info("Formulario creado id={} empresaId={}", created.getId(), empresaId);
    return toResponse(created);
  }

  public List<FormularioResponse> findByNodoId(String nodoId) {
    String empresaId = currentUserService.getEmpresaId();
    return formularioRepository.findByEmpresaIdAndNodoIdAndActivoTrue(empresaId, nodoId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public List<FormularioResponse> findByPoliticaId(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    return formularioRepository.findByEmpresaIdAndPoliticaIdAndActivoTrue(empresaId, politicaId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public List<FormularioResponse> list(String departamentoId, String q) {
    var currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    List<Formulario> base;
    if (currentUser.getRol().normalized() == Rol.ADMINISTRADOR_AREA) {
      if (currentUser.getDepartamentoId() == null || currentUser.getDepartamentoId().isBlank()) {
        return List.of();
      }
      base = formularioRepository.findByEmpresaIdAndDepartamentoIdAndActivoTrue(empresaId, currentUser.getDepartamentoId());
    } else {
      if (departamentoId != null && !departamentoId.isBlank()) {
        base = formularioRepository.findByEmpresaIdAndDepartamentoIdAndActivoTrue(empresaId, departamentoId);
      } else {
        base = formularioRepository.findByEmpresaIdAndActivoTrue(empresaId);
      }
    }
    String query = q == null ? "" : q.trim().toLowerCase();
    return base.stream()
        .filter(f -> query.isBlank()
            || (f.getNombre() != null && f.getNombre().toLowerCase().contains(query))
            || (f.getDescripcion() != null && f.getDescripcion().toLowerCase().contains(query)))
        .map(this::toResponse)
        .toList();
  }

  public FormularioResponse update(String id, FormularioRequest request) {
    var currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    Formulario current = formularioRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    enforceAdminOwnership(currentUser.getRol(), currentUser.getDepartamentoId(), current.getDepartamentoId());
    current.setPoliticaId(request.politicaId());
    current.setNodoId(request.nodoId());
    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setDepartamentoId(resolveDepartamentoId(request.departamentoId(), current.getDepartamentoId(), currentUser.getRol()));
    current.setCampos(mapCampos(request));
    Formulario updated = formularioRepository.save(current);
    log.info("Formulario actualizado id={} empresaId={}", id, empresaId);
    return toResponse(updated);
  }

  public void deactivate(String id) {
    var currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    Formulario current = formularioRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    enforceAdminOwnership(currentUser.getRol(), currentUser.getDepartamentoId(), current.getDepartamentoId());
    current.setActivo(false);
    formularioRepository.save(current);
    log.info("Formulario desactivado id={} empresaId={}", id, empresaId);
  }

  public FormularioResponse addCampo(String id, CampoRequest campoRequest) {
    var currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    Formulario current = formularioRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    enforceAdminOwnership(currentUser.getRol(), currentUser.getDepartamentoId(), current.getDepartamentoId());
    List<Campo> campos = current.getCampos() == null ? new ArrayList<>() : new ArrayList<>(current.getCampos());
    campos.add(Campo.builder()
        .nombre(campoRequest.nombre())
        .etiqueta(campoRequest.etiqueta())
        .tipo(campoRequest.tipo())
        .requerido(campoRequest.requerido())
        .opciones(campoRequest.opciones())
        .build());
    current.setCampos(campos);
    return toResponse(formularioRepository.save(current));
  }

  private List<Campo> mapCampos(FormularioRequest request) {
    return request.campos().stream()
        .map(item -> Campo.builder()
            .nombre(item.nombre())
            .etiqueta(item.etiqueta())
            .tipo(item.tipo())
            .requerido(item.requerido())
            .opciones(item.opciones())
            .build())
        .toList();
  }

  private FormularioResponse toResponse(Formulario item) {
    return new FormularioResponse(
        item.getId(),
        item.getPoliticaId(),
        item.getNodoId(),
        item.getNombre(),
        item.getDescripcion(),
        item.getDepartamentoId(),
        item.getCampos().stream()
            .map(campo -> new CampoResponse(
                campo.getNombre(),
                campo.getEtiqueta(),
                campo.getTipo().name(),
                campo.isRequerido(),
                campo.getOpciones()
            )).toList()
    );
  }

  private String resolveDepartamentoId(String requested, String currentUserDepartamentoId, Rol rol) {
    if (rol.normalized() == Rol.ADMINISTRADOR_AREA) {
      if (currentUserDepartamentoId == null || currentUserDepartamentoId.isBlank()) {
        throw new IllegalArgumentException("El usuario no tiene departamento asociado");
      }
      return currentUserDepartamentoId;
    }
    if (requested == null || requested.isBlank()) {
      throw new IllegalArgumentException("Debe indicar un departamento para el formulario");
    }
    return requested;
  }

  private void enforceAdminOwnership(Rol rol, String userDepartamentoId, String formularioDepartamentoId) {
    if (rol.normalized() != Rol.ADMINISTRADOR_AREA) return;
    if (userDepartamentoId == null || !userDepartamentoId.equals(formularioDepartamentoId)) {
      throw new IllegalArgumentException("No puede operar formularios de otro departamento");
    }
  }
}
