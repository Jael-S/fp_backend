package com.flowpolicy.notificacion.service;

import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.notificacion.dto.NotificacionResponse;
import com.flowpolicy.notificacion.model.Notificacion;
import com.flowpolicy.notificacion.repository.NotificacionRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

  private final NotificacionRepository notificacionRepository;
  private final CurrentUserService currentUserService;
  private final SimpMessagingTemplate messagingTemplate;

  public PageResponse<NotificacionResponse> list(int page, int size) {
    String empresaId = currentUserService.getEmpresaId();
    String usuarioId = currentUserService.getCurrentUser().getId();
    Page<Notificacion> result = notificacionRepository.findByEmpresaIdAndUsuarioIdOrderByCreadoEnDesc(
        empresaId,
        usuarioId,
        PageRequest.of(page, size)
    );
    return new PageResponse<>(
        result.map(this::toResponse).getContent(),
        result.getTotalElements(),
        result.getNumber(),
        result.getSize()
    );
  }

  public long countNoLeidas() {
    String empresaId = currentUserService.getEmpresaId();
    String usuarioId = currentUserService.getCurrentUser().getId();
    return notificacionRepository.countByEmpresaIdAndUsuarioIdAndLeidaFalse(empresaId, usuarioId);
  }

  public NotificacionResponse markAsRead(String id) {
    String empresaId = currentUserService.getEmpresaId();
    String usuarioId = currentUserService.getCurrentUser().getId();
    Notificacion current = notificacionRepository.findByIdAndEmpresaIdAndUsuarioId(id, empresaId, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada"));
    current.setLeida(true);
    current.setFechaLectura(LocalDateTime.now());
    return toResponse(notificacionRepository.save(current));
  }

  public void markAllAsRead() {
    String empresaId = currentUserService.getEmpresaId();
    String usuarioId = currentUserService.getCurrentUser().getId();
    Page<Notificacion> page = notificacionRepository.findByEmpresaIdAndUsuarioIdOrderByCreadoEnDesc(
        empresaId,
        usuarioId,
        PageRequest.of(0, 1000)
    );
    List<Notificacion> updated = page.getContent().stream().map(item -> {
      item.setLeida(true);
      item.setFechaLectura(LocalDateTime.now());
      return item;
    }).toList();
    notificacionRepository.saveAll(updated);
  }

  public void notifyUsers(String empresaId, List<String> userIds, String tramiteId, String titulo, String mensaje) {
    notifyUsers(empresaId, userIds, tramiteId, null, "ALERTA", titulo, mensaje);
  }

  public void notifyUsers(
      String empresaId,
      List<String> userIds,
      String tramiteId,
      String ejecucionId,
      String tipo,
      String titulo,
      String mensaje
  ) {
    LocalDateTime now = LocalDateTime.now();
    List<Notificacion> batch = userIds.stream()
        .distinct()
        .map(userId -> Notificacion.builder()
            .empresaId(empresaId)
            .usuarioId(userId)
            .tramiteId(tramiteId)
            .ejecucionId(ejecucionId)
            .tipo(tipo)
            .titulo(titulo)
            .mensaje(mensaje)
            .leida(false)
            .creadoEn(now)
            .build())
        .toList();
    notificacionRepository.saveAll(batch);
    batch.forEach(item -> messagingTemplate.convertAndSend("/topic/notificaciones/" + item.getUsuarioId(), toResponse(item)));
  }

  public void delete(String id) {
    String empresaId = currentUserService.getEmpresaId();
    String usuarioId = currentUserService.getCurrentUser().getId();
    notificacionRepository.deleteByIdAndEmpresaIdAndUsuarioId(id, empresaId, usuarioId);
  }

  private NotificacionResponse toResponse(Notificacion item) {
    return new NotificacionResponse(
        item.getId(),
        item.getTramiteId(),
        item.getTitulo(),
        item.getMensaje(),
        item.isLeida(),
        item.getFechaLectura(),
        item.getCreadoEn()
    );
  }
}
