package com.flowpolicy.cobertura.service;

import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.cobertura.dto.PuntoCoberturaRequest;
import com.flowpolicy.cobertura.dto.PuntoCoberturaResponse;
import com.flowpolicy.cobertura.model.PuntoCobertura;
import com.flowpolicy.cobertura.repository.PuntoCoberturaRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoberturaService {

  private final PuntoCoberturaRepository puntoCoberturaRepository;
  private final CurrentUserService currentUserService;
  private final SimpMessagingTemplate messagingTemplate;

  public List<PuntoCoberturaResponse> list() {
    var currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    Rol rol = currentUser.getRol().normalized();

    List<PuntoCobertura> points = (rol == Rol.ADMINISTRADOR_AREA)
        ? puntoCoberturaRepository.findByEmpresaIdAndDepartamentoIdAndActivoTrue(empresaId, currentUser.getDepartamentoId())
        : puntoCoberturaRepository.findByEmpresaIdAndActivoTrue(empresaId);

    return points.stream().map(this::toResponse).toList();
  }

  public PuntoCoberturaResponse create(PuntoCoberturaRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    PuntoCobertura created = puntoCoberturaRepository.save(PuntoCobertura.builder()
        .empresaId(empresaId)
        .departamentoId(request.departamentoId())
        .nombre(request.nombre())
        .tipo(request.tipo())
        .latitud(request.latitud())
        .longitud(request.longitud())
        .metadata(request.metadata())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    publishMapUpdate(empresaId);
    return toResponse(created);
  }

  public PuntoCoberturaResponse update(String id, PuntoCoberturaRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    PuntoCobertura current = puntoCoberturaRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Punto de cobertura no encontrado"));
    current.setDepartamentoId(request.departamentoId());
    current.setNombre(request.nombre());
    current.setTipo(request.tipo());
    current.setLatitud(request.latitud());
    current.setLongitud(request.longitud());
    current.setMetadata(request.metadata());
    PuntoCobertura updated = puntoCoberturaRepository.save(current);
    publishMapUpdate(empresaId);
    return toResponse(updated);
  }

  public void deactivate(String id) {
    String empresaId = currentUserService.getEmpresaId();
    PuntoCobertura current = puntoCoberturaRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Punto de cobertura no encontrado"));
    current.setActivo(false);
    puntoCoberturaRepository.save(current);
    publishMapUpdate(empresaId);
  }

  private PuntoCoberturaResponse toResponse(PuntoCobertura item) {
    return new PuntoCoberturaResponse(
        item.getId(),
        item.getEmpresaId(),
        item.getDepartamentoId(),
        item.getNombre(),
        item.getTipo(),
        item.getLatitud(),
        item.getLongitud(),
        item.getMetadata()
    );
  }

  private void publishMapUpdate(String empresaId) {
    messagingTemplate.convertAndSend("/topic/monitor/cobertura/" + empresaId, "MAPA_ACTUALIZADO");
  }
}
