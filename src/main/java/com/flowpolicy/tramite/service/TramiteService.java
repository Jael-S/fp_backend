package com.flowpolicy.tramite.service;

import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.departamento.repository.DepartamentoRepository;
import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import com.flowpolicy.ejecucion.repository.EjecucionNodoRepository;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.notificacion.service.NotificacionService;
import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.repository.PoliticaRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class TramiteService {

  private final TramiteRepository tramiteRepository;
  private final NodoRepository nodoRepository;
  private final EjecucionNodoRepository ejecucionNodoRepository;
  private final PoliticaRepository politicaRepository;
  private final DepartamentoRepository departamentoRepository;
  private final CurrentUserService currentUserService;
  private final UsuarioRepository usuarioRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final NotificacionService notificacionService;

  public TramiteResponse create(TramiteRequest request) {
    Usuario currentUser = currentUserService.getCurrentUser();
    String empresaId = currentUser.getEmpresaId();
    politicaRepository.findById(request.politicaId())
        .filter(p -> empresaId.equals(p.getEmpresaId()) && p.isActivo() && p.getEstado() == EstadoPolitica.ACTIVA)
        .orElseThrow(() -> new IllegalArgumentException("La politica seleccionada no esta ACTIVA"));

    Nodo inicio = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(request.politicaId(), empresaId)
        .stream()
        .filter(n -> n.getTipo() == TipoNodo.INICIO)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("La politica no tiene nodo INICIO"));

    String departamentoAsignado = inicio.getDepartamentoId() != null && !inicio.getDepartamentoId().isBlank()
        ? inicio.getDepartamentoId()
        : (request.departamentoId() != null ? request.departamentoId() : currentUser.getDepartamentoId());
    String prioridad = request.prioridad() == null || request.prioridad().isBlank() ? "MEDIA" : request.prioridad().toUpperCase();
    LocalDateTime now = LocalDateTime.now();
    String codigo = (request.codigoSeguimiento() != null && !request.codigoSeguimiento().isBlank())
        ? request.codigoSeguimiento()
        : generateTrackingCode(empresaId, now.getYear());
    Tramite created = tramiteRepository.save(Tramite.builder()
        .empresaId(empresaId)
        .departamentoId(departamentoAsignado)
        .politicaId(request.politicaId())
        .titulo(request.titulo())
        .prioridad(prioridad)
        .fechaLimite(request.fechaLimite())
        .usuarioCreadorId(currentUser.getId())
        .creadoPorNombre(currentUser.getNombre())
        .estado(EstadoTramite.PENDIENTE)
        .nodoActualId(inicio.getId())
        .departamentoActualId(departamentoAsignado)
        .clienteNombre(request.clienteNombre())
        .clienteIdentidad(request.clienteIdentidad())
        .clienteEmail(request.clienteEmail())
        .codigoSeguimiento(codigo)
        .datos(request.datos())
        .historial(new ArrayList<>(List.of(EventoTramite.builder()
            .fecha(now)
            .nodoNombre(inicio.getNombre())
            .departamentoNombre(resolveDepartamentoNombre(departamentoAsignado))
            .completado(false)
            .evento("TRAMITE_CREADO")
            .usuarioId(currentUser.getId())
            .detalles(Map.of(
                "politicaId", request.politicaId(),
                "titulo", request.titulo(),
                "prioridad", prioridad
            ))
            .build())))
        .activo(true)
        .creadoEn(now)
        .actualizadoEn(now)
        .build());

    String responsableId = departamentoRepository.findById(departamentoAsignado)
        .filter(d -> empresaId.equals(d.getEmpresaId()) && d.isActivo())
        .map(d -> d.getResponsableId())
        .orElse(currentUser.getId());
    ejecucionNodoRepository.save(EjecucionNodo.builder()
        .empresaId(empresaId)
        .departamentoId(departamentoAsignado)
        .tramiteId(created.getId())
        .nodoId(inicio.getId())
        .usuarioAsignadoId(responsableId)
        .estado(EstadoEjecucion.PENDIENTE)
        .activo(true)
        .creadoEn(now)
        .build());

    messagingTemplate.convertAndSend("/topic/monitoreo/tramites", toResponse(created));
    messagingTemplate.convertAndSend("/topic/monitor/" + created.getPoliticaId(), monitorByPolitica(created.getPoliticaId()));
    return toResponse(created);
  }

  public PageResponse<TramiteResponse> list(
      int page,
      int size,
      String politicaId,
      EstadoTramite estado,
      String departamentoId,
      String prioridad,
      String q,
      LocalDateTime fechaDesde,
      LocalDateTime fechaHasta
  ) {
    Usuario current = currentUserService.getCurrentUser();
    String empresaId = current.getEmpresaId();
    List<Tramite> base = current.getRol().normalized() == Rol.ADMINISTRADOR_AREA
        ? tramiteRepository.findByEmpresaIdAndDepartamentoIdAndActivoTrue(empresaId, current.getDepartamentoId())
        : tramiteRepository.findByEmpresaIdAndActivoTrue(empresaId);

    String prioridadFilter = prioridad == null ? "" : prioridad.trim().toUpperCase();
    String text = q == null ? "" : q.trim().toLowerCase();
    List<Tramite> filtered = base.stream()
        .filter(t -> politicaId == null || politicaId.isBlank() || politicaId.equals(t.getPoliticaId()))
        .filter(t -> estado == null || estado == t.getEstado())
        .filter(t -> departamentoId == null || departamentoId.isBlank() || departamentoId.equals(t.getDepartamentoId()))
        .filter(t -> prioridadFilter.isBlank() || prioridadFilter.equalsIgnoreCase(t.getPrioridad()))
        .filter(t -> fechaDesde == null || (t.getFechaLimite() != null && !t.getFechaLimite().isBefore(fechaDesde)))
        .filter(t -> fechaHasta == null || (t.getFechaLimite() != null && !t.getFechaLimite().isAfter(fechaHasta)))
        .filter(t -> text.isBlank()
            || (t.getTitulo() != null && t.getTitulo().toLowerCase().contains(text))
            || (t.getId() != null && t.getId().toLowerCase().contains(text)))
        .sorted(Comparator.comparing(Tramite::getCreadoEn, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();

    int from = Math.max(0, page * size);
    int to = Math.min(filtered.size(), from + size);
    List<TramiteResponse> pageItems = from >= filtered.size()
        ? List.of()
        : filtered.subList(from, to).stream().map(this::toResponse).toList();
    return new PageResponse<>(pageItems, filtered.size(), page, size);
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
    messagingTemplate.convertAndSend("/topic/monitor/" + tramite.getPoliticaId(), monitorByPolitica(tramite.getPoliticaId()));
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
    messagingTemplate.convertAndSend("/topic/monitor/" + tramite.getPoliticaId(), monitorByPolitica(tramite.getPoliticaId()));
  }

  public TramiteResponse updateEstado(String id, EstadoTramite estado) {
    String empresaId = currentUserService.getEmpresaId();
    Tramite current = tramiteRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Tramite no encontrado"));
    EstadoTramite destino = estado == null ? EstadoTramite.RECHAZADO : estado;
    if (destino == EstadoTramite.CANCELADO) {
      destino = EstadoTramite.RECHAZADO;
    }
    current.setEstado(destino);
    current.setActualizadoEn(LocalDateTime.now());
    if (current.getHistorial() == null) {
      current.setHistorial(new ArrayList<>());
    }
    current.getHistorial().add(EventoTramite.builder()
        .fecha(LocalDateTime.now())
        .evento("TRAMITE_ESTADO_ACTUALIZADO")
        .usuarioId(currentUserService.getCurrentUser().getId())
        .completado(destino == EstadoTramite.COMPLETADO)
        .detalles(Map.of("estado", destino.name()))
        .build());
    Tramite updated = tramiteRepository.save(current);
    messagingTemplate.convertAndSend("/topic/monitor/" + updated.getPoliticaId(), monitorByPolitica(updated.getPoliticaId()));
    return toResponse(updated);
  }

  public Map<String, Object> monitorByPolitica(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    List<Tramite> tramites = tramiteRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
        .filter(t -> politicaId.equals(t.getPoliticaId()))
        .toList();
    long total = tramites.size();
    long pendientes = tramites.stream().filter(t -> t.getEstado() == EstadoTramite.PENDIENTE).count();
    long enProceso = tramites.stream().filter(t -> t.getEstado() == EstadoTramite.EN_PROCESO).count();
    long completados = tramites.stream().filter(t -> t.getEstado() == EstadoTramite.COMPLETADO).count();
    long rechazados = tramites.stream().filter(t -> t.getEstado() == EstadoTramite.RECHAZADO || t.getEstado() == EstadoTramite.CANCELADO).count();
    Map<String, Long> porNodo = new HashMap<>();
    tramites.stream()
        .filter(t -> t.getNodoActualId() != null && !t.getNodoActualId().isBlank())
        .forEach(t -> porNodo.merge(t.getNodoActualId(), 1L, Long::sum));

    return Map.of(
        "politicaId", politicaId,
        "total", total,
        "pendientes", pendientes,
        "enProceso", enProceso,
        "completados", completados,
        "rechazados", rechazados,
        "porNodo", porNodo
    );
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
        item.getTitulo(),
        item.getPrioridad(),
        item.getFechaLimite(),
        item.getUsuarioCreadorId(),
        item.getCreadoPorNombre(),
        item.getEstado().name(),
        item.getNodoActualId(),
        item.getDepartamentoActualId(),
        item.getClienteNombre(),
        item.getClienteIdentidad(),
        item.getClienteEmail(),
        item.getCodigoSeguimiento(),
        item.getDatos(),
        item.getCreadoEn(),
        item.getActualizadoEn()
    );
  }

  private String generateTrackingCode(String empresaId, int year) {
    int sequence = 1;
    String code;
    do {
      code = "FP-" + year + "-" + String.format("%05d", sequence++);
    } while (tramiteRepository.findByEmpresaIdAndCodigoSeguimientoAndActivoTrue(empresaId, code).isPresent());
    return code;
  }

  private String resolveDepartamentoNombre(String departamentoId) {
    if (departamentoId == null || departamentoId.isBlank()) return "-";
    return departamentoRepository.findById(departamentoId)
        .map(d -> d.getNombre())
        .orElse(departamentoId);
  }
}
