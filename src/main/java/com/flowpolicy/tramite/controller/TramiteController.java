package com.flowpolicy.tramite.controller;

import com.flowpolicy.common.dto.ApiResponse;
import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.politica.model.Politica;
import com.flowpolicy.politica.repository.PoliticaRepository;
import com.flowpolicy.politica.dto.PoliticaResponse;
import com.flowpolicy.politica.service.PoliticaService;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.tramite.dto.TramiteDetalleResponse;
import com.flowpolicy.tramite.dto.TramiteRequest;
import com.flowpolicy.tramite.dto.TramiteResponse;
import com.flowpolicy.tramite.model.EstadoTramite;
import com.flowpolicy.tramite.model.EventoTramite;
import com.flowpolicy.tramite.service.TramiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tramites")
@RequiredArgsConstructor
public class TramiteController {

  private final TramiteService tramiteService;
  private final PoliticaService politicaService;
  private final PoliticaRepository politicaRepository;
  private final CurrentUserService currentUserService;

  @PostMapping
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<TramiteResponse> create(@Valid @RequestBody TramiteRequest request) {
    return ApiResponse.ok("Tramite creado", tramiteService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<PageResponse<TramiteResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String politicaId,
      @RequestParam(required = false) EstadoTramite estado,
      @RequestParam(required = false) String prioridad,
      @RequestParam(required = false) String departamentoId,
      @RequestParam(required = false) LocalDateTime fechaDesde,
      @RequestParam(required = false) LocalDateTime fechaHasta
  ) {
    return ApiResponse.ok(
        "Tramites listados",
        tramiteService.list(page, size, politicaId, estado, departamentoId, prioridad, q, fechaDesde, fechaHasta)
    );
  }

  @GetMapping("/mis-tramites")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA','FUNCIONARIO')")
  public ApiResponse<PageResponse<TramiteResponse>> myTramites(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ApiResponse.ok("Mis tramites listados", tramiteService.myTramites(page, size));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<TramiteDetalleResponse> getById(@PathVariable String id) {
    return ApiResponse.ok("Tramite encontrado", tramiteService.getById(id));
  }

  @GetMapping("/{id}/detalle-completo")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<TramiteDetalleResponse> getDetalleCompleto(@PathVariable String id) {
    return ApiResponse.ok("Detalle completo de tramite", tramiteService.getById(id));
  }

  @GetMapping("/politicas-activas")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<List<PoliticaResponse>> politicasActivas() {
    try {
      List<PoliticaResponse> activas = politicaService.listActivas();
      return ApiResponse.ok("Politicas activas listadas", activas);
    } catch (Exception ex) {
      // Fallback defensivo para evitar 500 si hay datos legacy con estado string.
      List<PoliticaResponse> activas = politicaRepository
          .findByEmpresaIdAndEstadoAndActivoTrue(
              currentUserService.getEmpresaId(),
              "ACTIVA"
          )
          .stream()
          .map(this::toPoliticaResponse)
          .toList();
      return ApiResponse.ok("Politicas activas listadas", activas);
    }
  }

  private PoliticaResponse toPoliticaResponse(Politica politica) {
    return new PoliticaResponse(
        politica.getId(),
        politica.getEmpresaId(),
        politica.getNombre(),
        politica.getDescripcion(),
        politica.getVersion(),
        politica.getEstado() == null ? "BORRADOR" : politica.getEstado().name(),
        politica.getCreadoPor(),
        politica.getDiagramaJson()
    );
  }

  @PutMapping("/{id}/estado")
  @PreAuthorize("hasRole('GESTOR_SISTEMA')")
  public ApiResponse<TramiteResponse> updateEstado(@PathVariable String id, @RequestBody java.util.Map<String, String> payload) {
    EstadoTramite estado = EstadoTramite.valueOf(payload.getOrDefault("estado", "RECHAZADO"));
    return ApiResponse.ok("Estado de tramite actualizado", tramiteService.updateEstado(id, estado));
  }

  @GetMapping("/{id}/historial")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<List<EventoTramite>> historial(@PathVariable String id) {
    return ApiResponse.ok("Historial de tramite", tramiteService.historial(id));
  }

  @GetMapping("/monitor/{politicaId}")
  @PreAuthorize("hasAnyRole('GESTOR_SISTEMA','ADMINISTRADOR_AREA')")
  public ApiResponse<Map<String, Object>> monitor(@PathVariable String politicaId) {
    return ApiResponse.ok("Estado de monitor obtenido", tramiteService.monitorByPolitica(politicaId));
  }
}
