package com.flowpolicy.transicion.service;

import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.politica.service.PoliticaService;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.transicion.dto.TransicionRequest;
import com.flowpolicy.transicion.dto.TransicionResponse;
import com.flowpolicy.transicion.model.Transicion;
import com.flowpolicy.transicion.repository.TransicionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransicionService {

  private final TransicionRepository transicionRepository;
  private final NodoRepository nodoRepository;
  private final PoliticaService politicaService;
  private final CurrentUserService currentUserService;

  public TransicionResponse create(String politicaId, TransicionRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    validateNodosEmpresa(request.nodoOrigenId(), request.nodoDestinoId(), empresaId);
    if (request.nodoOrigenId().equals(request.nodoDestinoId())) {
      throw new IllegalArgumentException("No se puede crear transicion al mismo nodo");
    }

    Transicion created = transicionRepository.save(Transicion.builder()
        .politicaId(politicaId)
        .empresaId(empresaId)
        .nodoOrigenId(request.nodoOrigenId())
        .nodoDestinoId(request.nodoDestinoId())
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .condicion(request.condicion())
        .tipo(request.tipo() == null || request.tipo().isBlank() ? "SECUENCIAL" : request.tipo())
        .etiqueta(request.etiqueta())
        .requiereAprobacion(Boolean.TRUE.equals(request.requiereAprobacion()))
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    politicaService.addTransicion(politicaId, created.getId());
    log.info("Transicion creada id={} politicaId={}", created.getId(), politicaId);
    return toResponse(created);
  }

  public List<TransicionResponse> listByPolitica(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    return transicionRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public TransicionResponse update(String id, TransicionRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    validateNodosEmpresa(request.nodoOrigenId(), request.nodoDestinoId(), empresaId);
    Transicion current = transicionRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Transicion no encontrada"));
    current.setNodoOrigenId(request.nodoOrigenId());
    current.setNodoDestinoId(request.nodoDestinoId());
    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setCondicion(request.condicion());
    if (request.tipo() != null && !request.tipo().isBlank()) {
      current.setTipo(request.tipo());
    }
    current.setEtiqueta(request.etiqueta());
    current.setRequiereAprobacion(Boolean.TRUE.equals(request.requiereAprobacion()));
    return toResponse(transicionRepository.save(current));
  }

  public void delete(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Transicion current = transicionRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Transicion no encontrada"));
    current.setActivo(false);
    transicionRepository.save(current);
  }

  private void validateNodosEmpresa(String origenId, String destinoId, String empresaId) {
    if (nodoRepository.findByIdAndEmpresaIdAndActivoTrue(origenId, empresaId).isEmpty()) {
      throw new ResourceNotFoundException("Nodo origen no encontrado");
    }
    if (nodoRepository.findByIdAndEmpresaIdAndActivoTrue(destinoId, empresaId).isEmpty()) {
      throw new ResourceNotFoundException("Nodo destino no encontrado");
    }
  }

  private TransicionResponse toResponse(Transicion item) {
    return new TransicionResponse(
        item.getId(),
        item.getPoliticaId(),
        item.getNodoOrigenId(),
        item.getNodoDestinoId(),
        item.getNombre(),
        item.getDescripcion(),
        item.getCondicion(),
        item.getTipo() == null || item.getTipo().isBlank() ? "SECUENCIAL" : item.getTipo(),
        item.getEtiqueta(),
        item.isRequiereAprobacion()
    );
  }
}
