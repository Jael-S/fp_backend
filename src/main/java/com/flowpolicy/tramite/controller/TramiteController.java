package com.flowpolicy.tramite.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.tramite.dto.TramiteDetalleResponse;
import com.flowpolicy.tramite.dto.TramiteRequest;
import com.flowpolicy.tramite.dto.TramiteResponse;
import com.flowpolicy.tramite.model.EstadoTramite;
import com.flowpolicy.tramite.model.EventoTramite;
import com.flowpolicy.tramite.service.TramiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tramites")
@RequiredArgsConstructor
public class TramiteController {

  private final TramiteService tramiteService;

  @PostMapping
  public ApiResponse<TramiteResponse> create(@Valid @RequestBody TramiteRequest request) {
    return ApiResponse.ok("Tramite creado", tramiteService.create(request));
  }

  @GetMapping
  public ApiResponse<PageResponse<TramiteResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String politicaId,
      @RequestParam(required = false) EstadoTramite estado,
      @RequestParam(required = false) String departamentoId,
      @RequestParam(required = false) LocalDateTime fechaDesde,
      @RequestParam(required = false) LocalDateTime fechaHasta
  ) {
    return ApiResponse.ok(
        "Tramites listados",
        tramiteService.list(page, size, politicaId, estado, departamentoId, fechaDesde, fechaHasta)
    );
  }

  @GetMapping("/mis-tramites")
  public ApiResponse<PageResponse<TramiteResponse>> myTramites(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ApiResponse.ok("Mis tramites listados", tramiteService.myTramites(page, size));
  }

  @GetMapping("/{id}")
  public ApiResponse<TramiteDetalleResponse> getById(@PathVariable String id) {
    return ApiResponse.ok("Tramite encontrado", tramiteService.getById(id));
  }

  @GetMapping("/{id}/historial")
  public ApiResponse<List<EventoTramite>> historial(@PathVariable String id) {
    return ApiResponse.ok("Historial de tramite", tramiteService.historial(id));
  }
}
