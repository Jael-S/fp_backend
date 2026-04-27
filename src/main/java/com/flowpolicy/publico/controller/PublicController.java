package com.flowpolicy.publico.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.publico.model.SeguimientoCliente;
import com.flowpolicy.publico.repository.SeguimientoClienteRepository;
import com.flowpolicy.tramite.model.Tramite;
import com.flowpolicy.tramite.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

  private final TramiteRepository tramiteRepository;
  private final SeguimientoClienteRepository seguimientoClienteRepository;

  @GetMapping("/seguimiento/{codigo}")
  public ApiResponse<Map<String, Object>> seguimiento(@PathVariable String codigo, jakarta.servlet.http.HttpServletRequest request) {
    Tramite tramite = tramiteRepository.findByCodigoSeguimientoAndActivoTrue(codigo)
        .orElseThrow(() -> new IllegalArgumentException("No se encontro tramite para ese codigo"));

    seguimientoClienteRepository.save(SeguimientoCliente.builder()
        .codigoSeguimiento(codigo)
        .ipCliente(resolveClientIp(request))
        .consultadoEn(LocalDateTime.now())
        .build());

    List<Map<String, Object>> historial = new java.util.ArrayList<>();
    if (tramite.getHistorial() != null) {
      historial = tramite.getHistorial().stream()
          .map(h -> {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("evento", h.getEvento() == null ? "-" : h.getEvento());
            row.put("fecha", h.getFecha());
            row.put("nodoNombre", h.getNodoNombre() == null ? "-" : h.getNodoNombre());
            row.put("departamentoNombre", h.getDepartamentoNombre() == null ? "-" : h.getDepartamentoNombre());
            row.put("completado", Boolean.TRUE.equals(h.getCompletado()));
            return row;
          })
          .toList();
    }

    return ApiResponse.ok("Seguimiento obtenido", Map.of(
        "codigoSeguimiento", tramite.getCodigoSeguimiento(),
        "titulo", tramite.getTitulo(),
        "estado", tramite.getEstado().name(),
        "departamentoActualId", tramite.getDepartamentoActualId(),
        "nodoActualId", tramite.getNodoActualId(),
        "clienteNombre", tramite.getClienteNombre(),
        "historial", historial
    ));
  }

  private String resolveClientIp(jakarta.servlet.http.HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
