package com.flowpolicy.ejecucion.service;

import com.flowpolicy.archivo.service.ArchivoStorageService;
import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.departamento.model.Departamento;
import com.flowpolicy.departamento.repository.DepartamentoRepository;
import com.flowpolicy.ejecucion.dto.EjecucionResponse;
import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import com.flowpolicy.ejecucion.repository.EjecucionNodoRepository;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.notificacion.service.NotificacionService;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.tramite.model.EstadoTramite;
import com.flowpolicy.tramite.model.Tramite;
import com.flowpolicy.tramite.repository.TramiteRepository;
import com.flowpolicy.tramite.service.TramiteService;
import com.flowpolicy.transicion.model.Transicion;
import com.flowpolicy.transicion.repository.TransicionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EjecucionService {

  private final EjecucionNodoRepository ejecucionNodoRepository;
  private final CurrentUserService currentUserService;
  private final ArchivoStorageService archivoStorageService;
  private final SimpMessagingTemplate messagingTemplate;
  private final TramiteService tramiteService;
  private final TramiteRepository tramiteRepository;
  private final UsuarioRepository usuarioRepository;
  private final NotificacionService notificacionService;
  private final TransicionRepository transicionRepository;
  private final NodoRepository nodoRepository;
  private final DepartamentoRepository departamentoRepository;

  public List<EjecucionResponse> pendientes() {
    Usuario current = currentUserService.getCurrentUser();
    String empresaId = current.getEmpresaId();
    List<EjecucionNodo> ejecuciones = ejecucionNodoRepository.findByEmpresaIdAndUsuarioAsignadoIdAndEstadoAndActivoTrue(
            empresaId,
            current.getId(),
            EstadoEjecucion.PENDIENTE
        );
    Map<String, Integer> prioridadTramite = new HashMap<>();
    List<String> tramiteIds = ejecuciones.stream().map(EjecucionNodo::getTramiteId).distinct().toList();
    List<Tramite> tramites = tramiteRepository.findAllById(tramiteIds);
    tramites.forEach(t -> prioridadTramite.put(t.getId(), prioridadRank(t.getPrioridad())));
    return ejecuciones.stream()
        .sorted(Comparator.comparing((EjecucionNodo e) -> prioridadTramite.getOrDefault(e.getTramiteId(), 99)))
        .map(this::toResponse)
        .toList();
  }

  /** Tareas ya completadas o rechazadas por el funcionario actual, ordenadas por más reciente. */
  public List<EjecucionResponse> historial() {
    Usuario current = currentUserService.getCurrentUser();
    String empresaId = current.getEmpresaId();
    return ejecucionNodoRepository
        .findByEmpresaIdAndUsuarioAsignadoIdAndActivoTrue(empresaId, current.getId())
        .stream()
        .filter(e -> e.getEstado() == EstadoEjecucion.COMPLETADO || e.getEstado() == EstadoEjecucion.RECHAZADO)
        .sorted(Comparator.comparing(EjecucionNodo::getFinEjecucion, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(this::toResponse)
        .toList();
  }

  public List<EjecucionResponse> byTramite(String tramiteId) {
    String empresaId = currentUserService.getEmpresaId();
    return ejecucionNodoRepository.findByEmpresaIdAndTramiteIdAndActivoTrue(empresaId, tramiteId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public EjecucionResponse getById(String id) {
    String empresaId = currentUserService.getEmpresaId();
    EjecucionNodo current = ejecucionNodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Ejecucion no encontrada"));
    validateAccess(current);
    return toResponse(current);
  }

  public EjecucionResponse iniciar(String id) {
    String empresaId = currentUserService.getEmpresaId();
    EjecucionNodo current = ejecucionNodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Ejecucion no encontrada"));
    validateOwner(current);
    current.setEstado(EstadoEjecucion.EN_PROCESO);
    current.setInicioEjecucion(LocalDateTime.now());
    EjecucionNodo updated = ejecucionNodoRepository.save(current);
    messagingTemplate.convertAndSend("/topic/monitoreo/ejecuciones", toResponse(updated));
    return toResponse(updated);
  }

  public EjecucionResponse completar(String id, String respuestasJson, MultipartFile[] archivos) {
    String empresaId = currentUserService.getEmpresaId();
    EjecucionNodo current = ejecucionNodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Ejecucion no encontrada"));
    validateOwner(current);
    current.setEstado(EstadoEjecucion.COMPLETADO);
    LocalDateTime now = LocalDateTime.now();
    if (current.getInicioEjecucion() == null) {
      current.setInicioEjecucion(now);
    }
    current.setFinEjecucion(now);
    current.setDuracionMs(java.time.Duration.between(current.getInicioEjecucion(), now).toMillis());
    current.setRespuestasFormulario(Map.of("payload", respuestasJson == null ? "{}" : respuestasJson));
    List<String> adjuntos = new ArrayList<>();
    if (archivos != null) {
      for (MultipartFile archivo : archivos) {
        String path = archivoStorageService.save(archivo);
        if (path != null) {
          adjuntos.add(path);
        }
      }
    }
    current.setAdjuntos(adjuntos);
    EjecucionNodo updated = ejecucionNodoRepository.save(current);
    notifyCompletion(updated);

    Tramite tramite = tramiteRepository.findByIdAndEmpresaIdAndActivoTrue(updated.getTramiteId(), empresaId).orElse(null);
    if (tramite != null
        && tramite.getEstado() != EstadoTramite.COMPLETADO
        && tramite.getEstado() != EstadoTramite.RECHAZADO) {
      avanzarTramitePorTransicion(tramite, updated, empresaId, respuestasJson);
    }

    messagingTemplate.convertAndSend("/topic/monitoreo/ejecuciones", toResponse(updated));
    return toResponse(updated);
  }

  private void avanzarTramitePorTransicion(Tramite tramite, EjecucionNodo ejecucionCompletada, String empresaId, String respuestasJson) {
    List<Transicion> outs = transicionRepository.findByPoliticaIdAndNodoOrigenIdAndEmpresaIdAndActivoTrue(
        tramite.getPoliticaId(),
        ejecucionCompletada.getNodoId(),
        empresaId
    );
    if (outs.isEmpty()) {
      log.debug("Sin transiciones salientes desde nodoMongoId={} tramite={}", ejecucionCompletada.getNodoId(), tramite.getId());
      return;
    }
    Transicion elegida = elegirTransicion(outs, respuestasJson);
    if (elegida == null) {
      return;
    }
    Nodo siguiente = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(elegida.getNodoDestinoId(), empresaId).orElse(null);
    if (siguiente == null) {
      log.warn("Nodo destino de transicion no encontrado destinoMongoId={}", elegida.getNodoDestinoId());
      return;
    }
    if (siguiente.getTipo() == TipoNodo.FIN) {
      tramiteService.markCompletedByExecution(tramite.getId(), currentUserService.getCurrentUser().getId());
      return;
    }

    String deptSig = siguiente.getDepartamentoId() != null && !siguiente.getDepartamentoId().isBlank()
        ? siguiente.getDepartamentoId()
        : tramite.getDepartamentoActualId();
    String responsableId = resolverResponsableDepartamento(empresaId, deptSig, tramite);

    tramite.setNodoActualId(siguiente.getId());
    tramite.setDepartamentoActualId(deptSig != null ? deptSig : tramite.getDepartamentoId());
    tramite.setEstado(EstadoTramite.EN_PROCESO);
    tramite.setActualizadoEn(LocalDateTime.now());
    tramiteRepository.save(tramite);

    ejecucionNodoRepository.save(EjecucionNodo.builder()
        .empresaId(empresaId)
        .departamentoId(deptSig != null ? deptSig : tramite.getDepartamentoId())
        .tramiteId(tramite.getId())
        .nodoId(siguiente.getId())
        .usuarioAsignadoId(responsableId)
        .estado(EstadoEjecucion.PENDIENTE)
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
  }

  private static Transicion elegirTransicion(List<Transicion> outs, String respuestasJson) {
    if (outs.isEmpty()) {
      return null;
    }
    if (outs.size() == 1) {
      return outs.get(0);
    }
    String payload = respuestasJson == null ? "" : respuestasJson.toLowerCase();
    for (Transicion t : outs) {
      String c = t.getCondicion();
      if (c == null || c.isBlank()) {
        return t;
      }
      if (payload.contains(c.toLowerCase())) {
        return t;
      }
    }
    return outs.get(0);
  }

  private String resolverResponsableDepartamento(String empresaId, String departamentoId, Tramite tramite) {
    if (departamentoId != null && !departamentoId.isBlank()) {
      // Buscar FUNCIONARIO activo del departamento primero
      var funcionarios = usuarioRepository.findByEmpresaIdAndDepartamentoIdAndRolAndActivoTrue(
          empresaId, departamentoId, Rol.FUNCIONARIO, PageRequest.of(0, 1));
      if (funcionarios.hasContent()) {
        return funcionarios.getContent().get(0).getId();
      }
      // Fallback: responsable del departamento (ADMIN_AREA)
      return departamentoRepository.findById(departamentoId)
          .filter(d -> empresaId.equals(d.getEmpresaId()) && d.isActivo())
          .map(Departamento::getResponsableId)
          .filter(id -> id != null && !id.isBlank())
          .orElse(tramite.getUsuarioCreadorId());
    }
    return tramite.getUsuarioCreadorId();
  }

  public EjecucionResponse rechazar(String id, String observaciones) {
    String empresaId = currentUserService.getEmpresaId();
    EjecucionNodo current = ejecucionNodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Ejecucion no encontrada"));
    validateOwner(current);
    current.setEstado(EstadoEjecucion.RECHAZADO);
    current.setObservaciones(observaciones);
    LocalDateTime now = LocalDateTime.now();
    if (current.getInicioEjecucion() == null) {
      current.setInicioEjecucion(now);
    }
    current.setFinEjecucion(now);
    current.setDuracionMs(java.time.Duration.between(current.getInicioEjecucion(), now).toMillis());
    EjecucionNodo updated = ejecucionNodoRepository.save(current);
    notifyRejection(updated, observaciones);
    tramiteService.markRejectedByExecution(updated.getTramiteId(), currentUserService.getCurrentUser().getId(), observaciones);
    messagingTemplate.convertAndSend("/topic/monitoreo/ejecuciones", toResponse(updated));
    return toResponse(updated);
  }

  private void validateAccess(EjecucionNodo ejecucion) {
    Usuario user = currentUserService.getCurrentUser();
    Rol rol = user.getRol().normalized();
    if (rol == Rol.GESTOR_SISTEMA) {
      return;
    }
    if (rol == Rol.ADMINISTRADOR_AREA && user.getDepartamentoId() != null
        && user.getDepartamentoId().equals(ejecucion.getDepartamentoId())) {
      return;
    }
    if (rol == Rol.FUNCIONARIO && user.getId().equals(ejecucion.getUsuarioAsignadoId())) {
      return;
    }
    throw new IllegalArgumentException("Sin permisos para esta ejecucion");
  }

  private void validateOwner(EjecucionNodo ejecucion) {
    Usuario user = currentUserService.getCurrentUser();
    if (!user.getId().equals(ejecucion.getUsuarioAsignadoId())) {
      throw new IllegalArgumentException("Solo el funcionario asignado puede ejecutar esta tarea");
    }
  }

  private void notifyCompletion(EjecucionNodo ejecucion) {
    String empresaId = currentUserService.getEmpresaId();
    List<String> gestores = usuarioRepository.findByEmpresaIdAndRolAndActivoTrue(empresaId, Rol.GESTOR_SISTEMA, PageRequest.of(0, 200))
        .getContent().stream().map(Usuario::getId).toList();
    List<String> adminsArea = usuarioRepository.findByEmpresaIdAndRolAndActivoTrue(empresaId, Rol.ADMINISTRADOR_AREA, PageRequest.of(0, 200))
        .getContent().stream().map(Usuario::getId).toList();
    List<String> destinatarios = new ArrayList<>();
    destinatarios.addAll(gestores);
    destinatarios.addAll(adminsArea);
    notificacionService.notifyUsers(
        empresaId,
        destinatarios,
        ejecucion.getTramiteId(),
        "Tarea completada",
        "La tarea " + ejecucion.getId() + " fue completada por un funcionario."
    );
  }

  private void notifyRejection(EjecucionNodo ejecucion, String observaciones) {
    String empresaId = currentUserService.getEmpresaId();
    List<String> gestores = usuarioRepository.findByEmpresaIdAndRolAndActivoTrue(empresaId, Rol.GESTOR_SISTEMA, PageRequest.of(0, 200))
        .getContent().stream().map(Usuario::getId).toList();
    notificacionService.notifyUsers(
        empresaId,
        gestores,
        ejecucion.getTramiteId(),
        "Tarea rechazada",
        "La tarea " + ejecucion.getId() + " fue rechazada. Obs: " + observaciones
    );
  }

  private EjecucionResponse toResponse(EjecucionNodo item) {
    String tramiteTitulo = tramiteRepository.findById(item.getTramiteId())
        .map(Tramite::getTitulo)
        .orElse(item.getTramiteId());

    String nodoNombre = nodoRepository.findById(item.getNodoId())
        .map(n -> n.getNombre() != null && !n.getNombre().isBlank() ? n.getNombre() : n.getTipo().name())
        .orElse(item.getNodoId());

    String departamentoNombre = (item.getDepartamentoId() != null && !item.getDepartamentoId().isBlank())
        ? departamentoRepository.findById(item.getDepartamentoId())
              .map(Departamento::getNombre)
              .orElse(null)
        : null;

    return new EjecucionResponse(
        item.getId(),
        item.getTramiteId(),
        tramiteTitulo,
        item.getNodoId(),
        nodoNombre,
        item.getDepartamentoId(),
        departamentoNombre,
        item.getUsuarioAsignadoId(),
        item.getEstado().name(),
        item.getInicioEjecucion(),
        item.getFinEjecucion(),
        item.getDuracionMs(),
        item.getRespuestasFormulario(),
        item.getAdjuntos(),
        item.getObservaciones()
    );
  }

  private int prioridadRank(String prioridad) {
    if (prioridad == null) return 2;
    return switch (prioridad.toUpperCase()) {
      case "ALTA" -> 0;
      case "MEDIA" -> 1;
      default -> 2;
    };
  }
}
