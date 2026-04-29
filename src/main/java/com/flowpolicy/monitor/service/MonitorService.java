package com.flowpolicy.monitor.service;

import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.departamento.repository.DepartamentoRepository;
import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import com.flowpolicy.ejecucion.repository.EjecucionNodoRepository;
import com.flowpolicy.monitor.dto.MonitorResponseDTO;
import com.flowpolicy.monitor.dto.TramiteMonitorDTO;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.tramite.model.EstadoTramite;
import com.flowpolicy.tramite.model.Tramite;
import com.flowpolicy.tramite.repository.TramiteRepository;
import com.flowpolicy.tramite.service.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorService {

  // ─── servicios existentes (NO se tocan) ────────────────────────────────────
  private final TramiteService tramiteService;

  // ─── repos adicionales ───────────────────────────────────────────────────────
  private final TramiteRepository tramiteRepository;
  private final EjecucionNodoRepository ejecucionNodoRepository;
  private final NodoRepository nodoRepository;
  private final UsuarioRepository usuarioRepository;
  private final DepartamentoRepository departamentoRepository;
  private final CurrentUserService currentUserService;
  private final SimpMessagingTemplate messagingTemplate;

  // ─────────────────────────────────────────────────────────────────────────────
  // API pública
  // ─────────────────────────────────────────────────────────────────────────────

  /** Endpoint HTTP: devuelve las estadísticas completas con nodos por elementId. */
  public MonitorResponseDTO estadoPolitica(String politicaId) {
    // Base de stats ya calculada y probada (TramiteService sigue igual)
    Map<String, Object> base = tramiteService.monitorByPolitica(politicaId);
    String empresaId = currentUserService.getEmpresaId();
    return enriquecerConNodos(politicaId, empresaId, base);
  }

  /**
   * Push WebSocket: llamar desde TramiteService / EjecucionService cuando
   * cambia el estado de un trámite para actualizar todos los clientes en tiempo real.
   */
  public void pushUpdate(String politicaId, String empresaId) {
    try {
      Map<String, Object> base = tramiteService.monitorByPolitica(politicaId);
      MonitorResponseDTO data = enriquecerConNodos(politicaId, empresaId, base);
      messagingTemplate.convertAndSend("/topic/monitor/" + politicaId, data);
    } catch (Exception ignored) {
      // No bloquear el flujo principal si el push WS falla
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Monitor por trámite (progreso de UN solo trámite)
  // ─────────────────────────────────────────────────────────────────────────────

  public TramiteMonitorDTO estadoTramite(String tramiteId) {
    String empresaId = currentUserService.getEmpresaId();

    Tramite tramite = tramiteRepository.findByIdAndEmpresaIdAndActivoTrue(tramiteId, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Tramite no encontrado"));

    // Nodos del diagrama de la política
    List<Nodo> nodos = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(
        tramite.getPoliticaId(), empresaId);
    Map<String, Nodo> nodoPorId = nodos.stream()
        .collect(Collectors.toMap(Nodo::getId, n -> n, (a, b) -> a));

    // Todas las ejecuciones del trámite
    List<EjecucionNodo> ejecuciones = ejecucionNodoRepository
        .findByEmpresaIdAndTramiteIdAndActivoTrue(empresaId, tramiteId);

    // Nodo actual en ejecución (PENDIENTE o EN_PROCESO)
    EjecucionNodo actual = ejecuciones.stream()
        .filter(e -> e.getEstado() == EstadoEjecucion.PENDIENTE
                  || e.getEstado() == EstadoEjecucion.EN_PROCESO)
        .findFirst().orElse(null);

    // Nombres resueltos
    String nodoActualNombre = (actual != null)
        ? nodoPorId.getOrDefault(actual.getNodoId(), new Nodo()).getNombre()
        : null;
    if (nodoActualNombre == null && tramite.getNodoActualId() != null) {
      nodoActualNombre = nodoRepository.findById(tramite.getNodoActualId())
          .map(n -> n.getNombre() != null ? n.getNombre() : n.getTipo().name())
          .orElse(null);
    }

    String departamentoNombre = (tramite.getDepartamentoActualId() != null)
        ? departamentoRepository.findById(tramite.getDepartamentoActualId())
              .map(d -> d.getNombre()).orElse(null)
        : null;

    String funcionarioNombre = (actual != null && actual.getUsuarioAsignadoId() != null)
        ? usuarioRepository.findById(actual.getUsuarioAsignadoId())
              .map(u -> u.getNombre()).orElse(null)
        : null;

    long tiempoMin = tramite.getCreadoEn() != null
        ? Duration.between(tramite.getCreadoEn(), LocalDateTime.now()).toMinutes()
        : 0;

    // IDs de nodos ya completados para este trámite
    Set<String> idsCompletados = ejecuciones.stream()
        .filter(e -> e.getEstado() == EstadoEjecucion.COMPLETADO)
        .map(EjecucionNodo::getNodoId)
        .collect(Collectors.toSet());
    String idActual = actual != null ? actual.getNodoId() : null;

    // Mapa elementId → estado (para colorear el diagrama)
    Map<String, String> estadoNodos = new LinkedHashMap<>();
    for (Nodo nodo : nodos) {
      if (nodo.getElementId() == null || nodo.getElementId().isBlank()) continue;
      String estadoNodo;
      if (nodo.getId().equals(idActual)) {
        estadoNodo = actual.getEstado() == EstadoEjecucion.EN_PROCESO ? "EN_PROCESO" : "PENDIENTE";
      } else if (idsCompletados.contains(nodo.getId())) {
        estadoNodo = "COMPLETADO";
      } else {
        estadoNodo = "PENDIENTE_FUTURO";
      }
      estadoNodos.put(nodo.getElementId(), estadoNodo);
    }

    // Historial de ejecuciones ordenado por fecha de inicio
    List<TramiteMonitorDTO.EjecucionHistorialInfo> historial = ejecuciones.stream()
        .sorted(Comparator.comparing(e -> e.getCreadoEn() != null ? e.getCreadoEn() : LocalDateTime.MIN))
        .map(e -> {
          Nodo n = nodoPorId.get(e.getNodoId());
          String nombre = n != null && n.getNombre() != null ? n.getNombre()
              : (n != null ? n.getTipo().name() : e.getNodoId());
          long durMin = e.getDuracionMs() != null ? e.getDuracionMs() / 60000
              : (e.getInicioEjecucion() != null && e.getFinEjecucion() != null
                  ? Duration.between(e.getInicioEjecucion(), e.getFinEjecucion()).toMinutes() : 0);
          return new TramiteMonitorDTO.EjecucionHistorialInfo(
              nombre, e.getEstado().name(), e.getInicioEjecucion(), e.getFinEjecucion(), durMin);
        })
        .toList();

    return new TramiteMonitorDTO(tramiteId, tramite.getTitulo(), tramite.getEstado().name(),
        nodoActualNombre, departamentoNombre, funcionarioNombre, tiempoMin, estadoNodos, historial);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Lógica de enriquecimiento (añade elementId, funcionarios y timing)
  // ─────────────────────────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private MonitorResponseDTO enriquecerConNodos(
      String politicaId,
      String empresaId,
      Map<String, Object> base
  ) {
    long total       = toLong(base.get("total"));
    long pendientes  = toLong(base.get("pendientes"));
    long enProceso   = toLong(base.get("enProceso"));
    long completados = toLong(base.get("completados"));
    long rechazados  = toLong(base.get("rechazados"));

    // Nodos del diagrama con su elementId BPMN
    List<Nodo> nodos = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);

    // Trámites activos → agrupar ejecuciones por nodoId (MongoDB _id)
    List<Tramite> activos = tramiteRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
        .filter(t -> politicaId.equals(t.getPoliticaId()))
        .filter(t -> t.getEstado() == EstadoTramite.PENDIENTE
                  || t.getEstado() == EstadoTramite.EN_PROCESO)
        .toList();

    Map<String, List<EjecucionNodo>> ejPorNodo = new HashMap<>();
    Set<String> userIds = new HashSet<>();

    for (Tramite tramite : activos) {
      ejecucionNodoRepository
          .findByEmpresaIdAndTramiteIdAndActivoTrue(empresaId, tramite.getId())
          .stream()
          .filter(e -> e.getEstado() == EstadoEjecucion.PENDIENTE
                    || e.getEstado() == EstadoEjecucion.EN_PROCESO)
          .forEach(e -> {
            ejPorNodo.computeIfAbsent(e.getNodoId(), k -> new ArrayList<>()).add(e);
            if (e.getUsuarioAsignadoId() != null) userIds.add(e.getUsuarioAsignadoId());
          });
    }

    // Nombres de funcionarios en batch
    Map<String, String> nombres = new HashMap<>();
    userIds.forEach(uid ->
        usuarioRepository.findById(uid).ifPresent(u -> nombres.put(uid, u.getNombre()))
    );

    // Construir mapa elementId → NodoMonitorInfo
    Map<String, MonitorResponseDTO.NodoMonitorInfo> nodosInfo = new LinkedHashMap<>();

    for (Nodo nodo : nodos) {
      if (nodo.getElementId() == null || nodo.getElementId().isBlank()) continue;

      List<EjecucionNodo> ejecs = ejPorNodo.getOrDefault(nodo.getId(), List.of());
      int count = ejecs.size();

      List<String> funcionarios = ejecs.stream()
          .map(e -> nombres.getOrDefault(e.getUsuarioAsignadoId(), e.getUsuarioAsignadoId()))
          .filter(Objects::nonNull)
          .distinct()
          .toList();

      long tiempoPromedio = (long) ejecs.stream()
          .mapToLong(e -> {
            LocalDateTime ini = e.getInicioEjecucion() != null
                ? e.getInicioEjecucion() : e.getCreadoEn();
            return ini == null ? 0 : Duration.between(ini, LocalDateTime.now()).toMinutes();
          })
          .average()
          .orElse(0);

      String estado;
      if (count == 0) {
        estado = "SIN_TRAMITES";
      } else if (ejecs.stream().anyMatch(e -> e.getEstado() == EstadoEjecucion.EN_PROCESO)) {
        estado = "EN_PROCESO";
      } else {
        estado = "PENDIENTE";
      }

      String nombreNodo = nodo.getNombre() != null && !nodo.getNombre().isBlank()
          ? nodo.getNombre()
          : (nodo.getTipo() != null ? nodo.getTipo().name() : "NODO");

      nodosInfo.put(nodo.getElementId(), new MonitorResponseDTO.NodoMonitorInfo(
          nodo.getId(), nodo.getElementId(), nombreNodo,
          nodo.getTipo() != null ? nodo.getTipo().name() : "PROCESO",
          count, funcionarios, tiempoPromedio, estado
      ));
    }

    return new MonitorResponseDTO(
        politicaId, total, pendientes, enProceso, completados, rechazados, nodosInfo
    );
  }

  private long toLong(Object val) {
    if (val == null) return 0L;
    if (val instanceof Long l) return l;
    if (val instanceof Number n) return n.longValue();
    try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return 0L; }
  }
}
