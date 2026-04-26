package com.flowpolicy.nodo.service;

import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.nodo.dto.NodoPosicionRequest;
import com.flowpolicy.nodo.dto.NodoRequest;
import com.flowpolicy.nodo.dto.NodoResponse;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.politica.service.PoliticaService;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodoService {

  private final NodoRepository nodoRepository;
  private final CurrentUserService currentUserService;
  private final PoliticaService politicaService;

  public NodoResponse create(String politicaId, NodoRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    validateInicioConstraint(null, politicaId, empresaId, request.tipo());
    Nodo created = nodoRepository.save(Nodo.builder()
        .politicaId(politicaId)
        .empresaId(empresaId)
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .tipo(request.tipo())
        .formularioId(request.formularioId())
        .posicionX(request.posicionX())
        .posicionY(request.posicionY())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    politicaService.addNodo(politicaId, created.getId());
    log.info("Nodo creado id={} politicaId={}", created.getId(), politicaId);
    return toResponse(created);
  }

  public List<NodoResponse> listByPolitica(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    return nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public NodoResponse update(String id, NodoRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    validateInicioConstraint(current, empresaId, request.tipo());
    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setTipo(request.tipo());
    current.setFormularioId(request.formularioId());
    current.setPosicionX(request.posicionX());
    current.setPosicionY(request.posicionY());
    return toResponse(nodoRepository.save(current));
  }

  public NodoResponse updatePosition(String id, NodoPosicionRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    current.setPosicionX(request.posicionX());
    current.setPosicionY(request.posicionY());
    return toResponse(nodoRepository.save(current));
  }

  public void delete(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    current.setActivo(false);
    nodoRepository.save(current);
  }

  public Map<String, String> getFormularioByNodoId(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    return Map.of(
        "nodoId", current.getId(),
        "formularioId", current.getFormularioId() == null ? "" : current.getFormularioId()
    );
  }

  private NodoResponse toResponse(Nodo item) {
    return new NodoResponse(
        item.getId(),
        item.getPoliticaId(),
        item.getNombre(),
        item.getDescripcion(),
        item.getTipo().name(),
        item.getFormularioId(),
        item.getPosicionX(),
        item.getPosicionY()
    );
  }

  private void validateInicioConstraint(String nodoId, String politicaId, String empresaId, com.flowpolicy.nodo.model.TipoNodo tipo) {
    if (tipo != com.flowpolicy.nodo.model.TipoNodo.INICIO) {
      return;
    }
    long totalInicio = nodoRepository.countByPoliticaIdAndEmpresaIdAndActivoTrueAndTipo(
        politicaId,
        empresaId,
        com.flowpolicy.nodo.model.TipoNodo.INICIO
    );
    if (totalInicio > 0 && nodoId == null) {
      throw new IllegalArgumentException("Solo se permite un nodo INICIO por politica");
    }
  }

  private void validateInicioConstraint(Nodo current, String empresaId, com.flowpolicy.nodo.model.TipoNodo nuevoTipo) {
    if (nuevoTipo != com.flowpolicy.nodo.model.TipoNodo.INICIO) {
      return;
    }
    long totalInicio = nodoRepository.countByPoliticaIdAndEmpresaIdAndActivoTrueAndTipo(
        current.getPoliticaId(),
        empresaId,
        com.flowpolicy.nodo.model.TipoNodo.INICIO
    );
    boolean currentIsInicio = current.getTipo() == com.flowpolicy.nodo.model.TipoNodo.INICIO;
    if (totalInicio > 0 && !currentIsInicio) {
      throw new IllegalArgumentException("Solo se permite un nodo INICIO por politica");
    }
  }
}
