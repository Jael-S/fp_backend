package com.flowpolicy.tramite.service;

import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import com.flowpolicy.ejecucion.repository.EjecucionNodoRepository;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.notificacion.service.NotificacionService;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.tramite.dto.TramiteDetalleResponse;
import com.flowpolicy.tramite.dto.TramiteRequest;
import com.flowpolicy.tramite.dto.TramiteResponse;
import com.flowpolicy.tramite.model.EstadoTramite;
import com.flowpolicy.tramite.model.EventoTramite;
import com.flowpolicy.tramite.model.Tramite;
import com.flowpolicy.tramite.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TramiteService {

  private final TramiteRepository tramiteRepository;
  private final NodoRepository nodoRepository;
  private final EjecucionNodoRepository ejecucionNodoRepository;
  private final CurrentUserService currentUserService;
  private final UsuarioRepository usuarioRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final NotificacionService notificacionService;

  public TramiteResponse create(TramiteRequest request) {
    Usuario currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    Nodo inicio = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(request.politicaId(), empresaId)
        .stream()
        .filter(n -> n.getTipo() == TipoNodo.INICIO)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("La politica no tiene nodo INICIO"));

    LocalDateTime now = LocalDateTime.now();
    Tramite created = tramiteRepository.save(Tramite.builder()
        .empresaId(empresaId)
        .departamentoId(request.departamentoId() != null ? request.departamentoId() : currentUser.getDepartamentoId())
        .politicaId(request.politicaId())
        .usuarioCreadorId(currentUser.getId())
        .estado(EstadoTramite.PENDIENTE)
        .nodoActualId(inicio.getId())
        .datos(request.datos())
        .historial(new ArrayList<>(List.of(EventoTramite.builder()
            .fecha(now)
            .evento("TRAMITE_CREADO")
            .usuarioId(currentUser.getId())
            .detalles(Map.of("politicaId", request.politicaId()))
            .build())))
        .activo(true)
        .creadoEn(now)
        .actualizadoEn(now)
        .build());

    ejecucionNodoRepository.save(EjecucionNodo.builder()
        .empresaId(empresaId)
        .departamentoId(created.getDepartamentoId())
        .tramiteId(created.getId())
        .nodoId(inicio.getId())
        .usuarioAsignadoId(currentUser.getId())
        .estado(EstadoEjecucion.PENDIENTE)
        .activo(true)
        .creadoEn(now)
        .build());

    messagingTemplate.convertAndSend("/topic/monitoreo/tramites", toResponse(created));
    return toResponse(created);
  }

  public PageResponse<TramiteResponse> list(int page, int size, String politicaId, EstadoTramite estado, String departamentoId, LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
    Usuario current = currentUserService.getCurrentUser();
    String empresaId = current.getEmpresaId();
    PageRequest pageable = PageRequest.of(page, size);

    Page<Tramite> result;
    if (current.getRol().normalized() == Rol.FUNCIONARIO) {
      result = tramiteRepository.findByEmpresaIdAndUsuarioCreadorIdAndActivoTrue(empresaId, current.getId(), pageable);
    } else if (current.getRol().normalized() == Rol.ADMINISTRADOR_AREA) {
      result = tramiteRepository.findByEmpresaIdAndDepartamentoIdAndActivoTrue(empresaId, current.getDepartamentoId(), pageable);
    } else if (estado != null) {
      result = tramiteRepository.findByEmpresaIdAndEstadoAndActivoTrue(empresaId, estado, pageable);
    } else if (politicaId != null && !politicaId.isBlank()) {
      result = tramiteRepository.findByEmpresaIdAndPoliticaIdAndActivoTrue(empresaId, politicaId, pageable);
    } else if (departamentoId != null && !departamentoId.isBlank()) {
      result = tramiteRepository.findByEmpresaIdAndDepartamentoIdAndActivoTrue(empresaId, departamentoId, pageable);
    } else if (fechaDesde != null && fechaHasta != null) {
      result = tramiteRepository.findByEmpresaIdAndCreadoEnBetweenAndActivoTrue(empresaId, fechaDesde, fechaHasta, pageable);
    } else {
      result = tramiteRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable);
    }

    return new PageResponse<>(
        result.map(this::toResponse).getContent(),
        result.getTotalElements(),
        result.getNumber(),
        result.getSize()
    );
  }

  public TramiteDetalleResponse getById(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Tramite current = tramiteRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Tramite no encontrado"));
    validateVisibility(current);
    return new TramiteDetalleResponse(toResponse(current), current.getHistorial() == null ? List.of() : current.getHistorial());
  }

  public List<EventoTramite> historial(String id) {
    return getById(id).historial();
  }

  public PageResponse<TramiteResponse> myTramites(int page, int size) {
    Usuario currentUser = currentUserService.getCurrentUser();
    Page<Tramite> result = tramiteRepository.findByEmpresaIdAndUsuarioCreadorIdAndActivoTrue(
        currentUser.getEmpresaId(),
        currentUser.getId(),
        PageRequest.of(page, size)
    );
    return new PageResponse<>(
        result.map(this::toResponse).getContent(),
        result.getTotalElements(),
        result.getNumber(),
        result.getSize()
    );
  }

  public void markCompletedByExecution(String tramiteId, String userId) {
    String empresaId = currentUserService.getEmpresaId();
    Tramite tramite = tramiteRepository.findByIdAndEmpresaIdAndActivoTrue(tramiteId, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Tramite no encontrado"));
    tramite.setEstado(EstadoTramite.COMPLETADO);
    tramite.setActualizadoEn(LocalDateTime.now());
    if (tramite.getHistorial() == null) {
      tramite.setHistorial(new ArrayList<>());
    }
    tramite.getHistorial().add(EventoTramite.builder()
        .fecha(LocalDateTime.now())
        .evento("TRAMITE_COMPLETADO")
        .usuarioId(userId)
        .detalles(Map.of())
        .build());
    tramiteRepository.save(tramite);

    List<String> gestores = usuarioRepository.findByEmpresaIdAndRolAndActivoTrue(empresaId, Rol.GESTOR_SISTEMA, PageRequest.of(0, 100))
        .getContent()
        .stream()
        .map(Usuario::getId)
        .toList();
    notificacionService.notifyUsers(empresaId, gestores, tramiteId, "Tramite completado", "El tramite " + tramiteId + " fue completado.");
    messagingTemplate.convertAndSend("/topic/monitoreo/tramites", toResponse(tramite));
  }

  public void markRejectedByExecution(String tramiteId, String userId, String observaciones) {
    String empresaId = currentUserService.getEmpresaId();
    Tramite tramite = tramiteRepository.findByIdAndEmpresaIdAndActivoTrue(tramiteId, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Tramite no encontrado"));
    tramite.setEstado(EstadoTramite.RECHAZADO);
    tramite.setActualizadoEn(LocalDateTime.now());
    if (tramite.getHistorial() == null) {
      tramite.setHistorial(new ArrayList<>());
    }
    tramite.getHistorial().add(EventoTramite.builder()
        .fecha(LocalDateTime.now())
        .evento("TRAMITE_RECHAZADO")
        .usuarioId(userId)
        .detalles(Map.of("observaciones", observaciones))
        .build());
    tramiteRepository.save(tramite);

    List<String> gestores = usuarioRepository.findByEmpresaIdAndRolAndActivoTrue(empresaId, Rol.GESTOR_SISTEMA, PageRequest.of(0, 100))
        .getContent()
        .stream()
        .map(Usuario::getId)
        .toList();
    notificacionService.notifyUsers(empresaId, gestores, tramiteId, "Tramite rechazado", "El tramite " + tramiteId + " fue rechazado.");
    messagingTemplate.convertAndSend("/topic/monitoreo/tramites", toResponse(tramite));
  }

  private void validateVisibility(Tramite tramite) {
    Usuario current = currentUserService.getCurrentUser();
    Rol rol = current.getRol().normalized();
    if (rol == Rol.GESTOR_SISTEMA) {
      return;
    }
    if (rol == Rol.ADMINISTRADOR_AREA && current.getDepartamentoId() != null
        && current.getDepartamentoId().equals(tramite.getDepartamentoId())) {
      return;
    }
    if (rol == Rol.FUNCIONARIO && current.getId().equals(tramite.getUsuarioCreadorId())) {
      return;
    }
    throw new IllegalArgumentException("No tienes permisos para ver este tramite");
  }

  private TramiteResponse toResponse(Tramite item) {
    return new TramiteResponse(
        item.getId(),
        item.getEmpresaId(),
        item.getDepartamentoId(),
        item.getPoliticaId(),
        item.getUsuarioCreadorId(),
        item.getEstado().name(),
        item.getNodoActualId(),
        item.getDatos(),
        item.getCreadoEn(),
        item.getActualizadoEn()
    );
  }
}
