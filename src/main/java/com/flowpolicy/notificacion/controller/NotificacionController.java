package com.flowpolicy.notificacion.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.notificacion.dto.NoLeidasResponse;
import com.flowpolicy.notificacion.dto.NotificacionResponse;
import com.flowpolicy.notificacion.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

  private final NotificacionService notificacionService;

  @GetMapping
  public ApiResponse<PageResponse<NotificacionResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return ApiResponse.ok("Notificaciones listadas", notificacionService.list(page, size));
  }

  @GetMapping("/no-leidas")
  public ApiResponse<NoLeidasResponse> countNoLeidas() {
    return ApiResponse.ok("Contador no leidas", new NoLeidasResponse(notificacionService.countNoLeidas()));
  }

  @GetMapping("/no-leidas/count")
  public ApiResponse<NoLeidasResponse> countNoLeidasCompat() {
    return ApiResponse.ok("Contador no leidas", new NoLeidasResponse(notificacionService.countNoLeidas()));
  }

  @PutMapping("/{id}/leer")
  public ApiResponse<NotificacionResponse> markAsRead(@PathVariable String id) {
    return ApiResponse.ok("Notificacion leida", notificacionService.markAsRead(id));
  }

  @PutMapping("/leer-todas")
  public ApiResponse<Void> markAllAsRead() {
    notificacionService.markAllAsRead();
    return ApiResponse.ok("Notificaciones marcadas como leidas", null);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    notificacionService.delete(id);
    return ApiResponse.ok("Notificacion eliminada", null);
  }
}
