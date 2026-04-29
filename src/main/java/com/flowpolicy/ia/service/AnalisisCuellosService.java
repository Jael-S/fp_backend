package com.flowpolicy.ia.service;

import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import com.flowpolicy.ejecucion.repository.EjecucionNodoRepository;
import com.flowpolicy.ia.dto.CuelloBotellaDto;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.tramite.model.Tramite;
import com.flowpolicy.tramite.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalisisCuellosService {

  private final NodoRepository          nodoRepository;
  private final TramiteRepository       tramiteRepository;
  private final EjecucionNodoRepository ejecucionNodoRepository;
  private final CurrentUserService      currentUserService;

  private static final double UMBRAL_ALTO  = 30.0;   // > 30 min → ALTO
  private static final double UMBRAL_MEDIO = 15.0;   // 15-30 min → MEDIO
                                                      // < 15 min  → no mostrar

  public CuelloBotellaDto analizar(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();

    // ── 1. Validaciones iniciales ─────────────────────────────────────────────
    List<Nodo> nodos = nodoRepository
        .findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);
    if (nodos.isEmpty()) {
      return vacio("No hay nodos en esta política. Dibuja el diagrama primero.");
    }

    List<Tramite> tramites = tramiteRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
        .filter(t -> politicaId.equals(t.getPoliticaId()))
        .toList();
    if (tramites.isEmpty()) {
      return vacio("No hay trámites para esta política. Crea al menos un trámite.");
    }

    // ── 2. Recopilar ejecuciones COMPLETADAS con tiempo real ─────────────────
    //       (agrupadas por nodoId para evitar N+1)
    Map<String, List<EjecucionNodo>> completadasPorNodo = new HashMap<>();
    for (Tramite tramite : tramites) {
      ejecucionNodoRepository
          .findByEmpresaIdAndTramiteIdAndActivoTrue(empresaId, tramite.getId())
          .stream()
          .filter(e -> e.getEstado() == EstadoEjecucion.COMPLETADO
              && tienetiempoReal(e))           // solo ejecuciones con tiempo medible
          .forEach(e -> completadasPorNodo
              .computeIfAbsent(e.getNodoId(), k -> new ArrayList<>())
              .add(e));
    }

    if (completadasPorNodo.isEmpty()) {
      return vacio("No hay ejecuciones completadas aún. "
          + "Los funcionarios deben completar sus tareas para generar datos de análisis.");
    }

    // ── 3. Calcular métricas por nodo PROCESO ─────────────────────────────────
    List<CuelloBotellaDto.NodoCuello> resultados = new ArrayList<>();

    for (Nodo nodo : nodos) {
      // Solo tareas ejecutables — excluir INICIO, DECISION, FIN
      if (nodo.getTipo() != TipoNodo.PROCESO) continue;

      List<EjecucionNodo> completadas = completadasPorNodo.getOrDefault(nodo.getId(), List.of());
      if (completadas.isEmpty()) continue;   // regla: no mostrar sin ejecuciones

      // Tiempo promedio = suma(tiempos) / cantidad
      double tiempoPromedio = completadas.stream()
          .mapToDouble(this::tiempoMinutos)
          .average()
          .orElse(0);

      if (tiempoPromedio <= 0) continue;     // descarte de seguridad

      // Regla: no mostrar nodos con tiempo < 15 min
      if (tiempoPromedio < UMBRAL_MEDIO) continue;

      String nombre = (nodo.getNombre() != null && !nodo.getNombre().isBlank())
          ? nodo.getNombre() : "Tarea";

      String nivel;
      String sugerencia;
      long mins = Math.round(tiempoPromedio);

      if (tiempoPromedio > UMBRAL_ALTO) {
        nivel      = "ALTO";
        sugerencia = String.format(
            "🔴 Intervención inmediata: Reducir tiempo de '%s' (%d min)", nombre, mins);
      } else {
        nivel      = "MEDIO";
        sugerencia = String.format(
            "🟡 Monitorear: '%s' tarda %d minutos en promedio", nombre, mins);
      }

      log.info("[Análisis] '{}' → {:.1f} min | {} ejecuciones | {}",
          nombre, tiempoPromedio, completadas.size(), nivel);

      resultados.add(new CuelloBotellaDto.NodoCuello(
          nodo.getId(),
          nombre,
          Math.round(tiempoPromedio * 10.0) / 10.0,  // 1 decimal
          completadas.size(),
          nivel,
          sugerencia));
    }

    // ── 4. Ordenar de mayor a menor tiempo ────────────────────────────────────
    resultados.sort(Comparator
        .comparingDouble(CuelloBotellaDto.NodoCuello::tiempoPromedioMinutos)
        .reversed());

    if (resultados.isEmpty()) {
      return vacio("No se detectaron cuellos de botella. "
          + "Todos los nodos están por debajo de " + (int) UMBRAL_MEDIO + " minutos.");
    }

    // ── 5. Promedio global y mensaje resumen ──────────────────────────────────
    double promedioGlobal = resultados.stream()
        .mapToDouble(CuelloBotellaDto.NodoCuello::tiempoPromedioMinutos)
        .average()
        .orElse(0);

    long nAlto  = resultados.stream().filter(r -> "ALTO".equals(r.nivelRiesgo())).count();
    long nMedio = resultados.size() - nAlto;

    String mensaje = String.format(
        "Análisis completado. %d nodo(s) detectados: %d ALTO, %d MEDIO. "
            + "Tiempo promedio global: %.1f min.",
        resultados.size(), nAlto, nMedio, promedioGlobal);

    return new CuelloBotellaDto(resultados, Math.round(promedioGlobal * 10.0) / 10.0, mensaje);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  /** Devuelve true si la ejecución tiene duración medible. */
  private boolean tienetiempoReal(EjecucionNodo e) {
    if (e.getDuracionMs() != null && e.getDuracionMs() > 0) return true;
    return e.getInicioEjecucion() != null && e.getFinEjecucion() != null;
  }

  /** Calcula la duración en minutos de una ejecución completada. */
  private double tiempoMinutos(EjecucionNodo e) {
    if (e.getDuracionMs() != null && e.getDuracionMs() > 0)
      return e.getDuracionMs() / 60_000.0;
    if (e.getInicioEjecucion() != null && e.getFinEjecucion() != null)
      return Duration.between(e.getInicioEjecucion(), e.getFinEjecucion()).toSeconds() / 60.0;
    return 0;
  }

  private CuelloBotellaDto vacio(String mensaje) {
    return new CuelloBotellaDto(List.of(), 0, mensaje);
  }
}
