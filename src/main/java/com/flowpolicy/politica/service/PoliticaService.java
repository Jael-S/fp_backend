package com.flowpolicy.politica.service;

import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.politica.dto.PoliticaRequest;
import com.flowpolicy.politica.dto.PoliticaResponse;
import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.model.Politica;
import com.flowpolicy.politica.repository.PoliticaRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoliticaService {

  private final PoliticaRepository politicaRepository;
  private final CurrentUserService currentUserService;

  public PoliticaResponse create(PoliticaRequest request) {
    var currentUser = currentUserService.getCurrentUser();
    Politica created = politicaRepository.save(Politica.builder()
        .empresaId(currentUser.getEmpresaId())
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .diagramaJson(request.diagramaJson())
        .nodoIds(new ArrayList<>())
        .transicionIds(new ArrayList<>())
        .version(1)
        .estado(EstadoPolitica.BORRADOR)
        .creadoPor(currentUser.getId())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    log.info("Politica creada id={} empresaId={}", created.getId(), created.getEmpresaId());
    return toResponse(created);
  }

  public PoliticaResponse update(String id, PoliticaRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    if (current.getEstado() == EstadoPolitica.ACTIVA) {
      Politica newVersion = Politica.builder()
          .empresaId(current.getEmpresaId())
          .nombre(request.nombre())
          .descripcion(request.descripcion())
          .diagramaJson(request.diagramaJson())
          .nodoIds(new ArrayList<>(current.getNodoIds() == null ? new ArrayList<>() : current.getNodoIds()))
          .transicionIds(new ArrayList<>(current.getTransicionIds() == null ? new ArrayList<>() : current.getTransicionIds()))
          .version(current.getVersion() + 1)
          .estado(EstadoPolitica.BORRADOR)
          .creadoPor(currentUserService.getCurrentUser().getId())
          .activo(true)
          .creadoEn(LocalDateTime.now())
          .build();
      Politica created = politicaRepository.save(newVersion);
      log.info("Politica versionada idOrigen={} nuevaId={} version={}", id, created.getId(), created.getVersion());
      return toResponse(created);
    }

    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setDiagramaJson(request.diagramaJson());
    Politica updated = politicaRepository.save(current);
    log.info("Politica actualizada id={} empresaId={}", updated.getId(), empresaId);
    return toResponse(updated);
  }

  public PoliticaResponse activate(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    validateCanActivate(current);
    current.setEstado(EstadoPolitica.ACTIVA);
    Politica updated = politicaRepository.save(current);
    log.info("Politica activada id={} empresaId={}", updated.getId(), empresaId);
    return toResponse(updated);
  }

  public PoliticaResponse deactivate(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    current.setEstado(EstadoPolitica.BORRADOR);
    return toResponse(politicaRepository.save(current));
  }

  public PoliticaResponse getById(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    return toResponse(current);
  }

  public void delete(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    current.setActivo(false);
    politicaRepository.save(current);
  }

  public PageResponse<PoliticaResponse> list(int page, int size, EstadoPolitica estado) {
    String empresaId = currentUserService.getEmpresaId();
    Page<Politica> result = (estado == null)
        ? politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(page, size))
        : politicaRepository.findByEmpresaIdAndEstadoAndActivoTrue(empresaId, estado, PageRequest.of(page, size));
    return new PageResponse<>(
        result.map(this::toResponse).getContent(),
        result.getTotalElements(),
        result.getNumber(),
        result.getSize()
    );
  }

  private void validateCanActivate(Politica politica) {
    if (politica.getNodoIds() == null || politica.getNodoIds().isEmpty()) {
      throw new IllegalArgumentException("La politica debe tener nodos para activarse");
    }
    String diagrama = politica.getDiagramaJson();
    if (diagrama == null || diagrama.isBlank()) {
      return;
    }
    boolean hasInicio = diagrama.contains("INICIO");
    boolean hasFin = diagrama.contains("FIN");
    if (!hasInicio || !hasFin) {
      throw new IllegalArgumentException("La politica debe contener nodo INICIO y FIN");
    }
  }

  public void ensureBelongsToEmpresa(String politicaId, String empresaId) {
    politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
  }

  public void addNodo(String politicaId, String nodoId) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    if (politica.getNodoIds() == null) {
      politica.setNodoIds(new ArrayList<>());
    }
    if (!politica.getNodoIds().contains(nodoId)) {
      politica.getNodoIds().add(nodoId);
      politicaRepository.save(politica);
    }
  }

  public void addTransicion(String politicaId, String transicionId) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    if (politica.getTransicionIds() == null) {
      politica.setTransicionIds(new ArrayList<>());
    }
    if (!politica.getTransicionIds().contains(transicionId)) {
      politica.getTransicionIds().add(transicionId);
      politicaRepository.save(politica);
    }
  }

  private PoliticaResponse toResponse(Politica politica) {
    return new PoliticaResponse(
        politica.getId(),
        politica.getEmpresaId(),
        politica.getNombre(),
        politica.getDescripcion(),
        politica.getVersion(),
        politica.getEstado().name(),
        politica.getCreadoPor(),
        politica.getDiagramaJson()
    );
  }
}
