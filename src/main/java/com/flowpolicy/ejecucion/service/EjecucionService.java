package com.flowpolicy.ejecucion.service;

import com.flowpolicy.archivo.service.ArchivoStorageService;
import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.ejecucion.dto.EjecucionResponse;
import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import com.flowpolicy.ejecucion.repository.EjecucionNodoRepository;
import com.flowpolicy.notificacion.service.NotificacionService;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.tramite.model.Tramite;
import com.flowpolicy.tramite.repository.TramiteRepository;
import com.flowpolicy.tramite.service.TramiteService;
import lombok.RequiredArgsConstructor;
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
    tramiteService.markCompletedByExecution(updated.getTramiteId(), currentUserService.getCurrentUser().getId());
    messagingTemplate.convertAndSend("/topic/monitoreo/ejecuciones", toResponse(updated));
    return toResponse(updated);
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
    return new EjecucionResponse(
        item.getId(),
        item.getTramiteId(),
        item.getNodoId(),
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
