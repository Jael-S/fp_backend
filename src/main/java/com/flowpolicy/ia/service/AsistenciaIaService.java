package com.flowpolicy.ia.service;

import com.flowpolicy.departamento.model.Departamento;
import com.flowpolicy.departamento.repository.DepartamentoRepository;
import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import com.flowpolicy.ejecucion.repository.EjecucionNodoRepository;
import com.flowpolicy.ia.dto.AsistenciaIaResponse;
import com.flowpolicy.ia.dto.CuelloBotellaResponse;
import com.flowpolicy.ia.dto.GenerarDiagramaResponse;
import com.flowpolicy.ia.dto.GenerarFormularioResponse;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.repository.PoliticaRepository;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.tramite.model.Tramite;
import com.flowpolicy.tramite.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaIaService {

  @Value("${ia.service.url:http://localhost:5000}")
  private String iaServiceUrl;

  // ── Repositorios ─────────────────────────────────────────────────────────────
  private final CurrentUserService currentUserService;
  private final PoliticaRepository politicaRepository;
  private final TramiteRepository tramiteRepository;
  private final EjecucionNodoRepository ejecucionNodoRepository;
  private final NodoRepository nodoRepository;
  private final DepartamentoRepository departamentoRepository;
  private final RestTemplate restTemplate;

  // ─────────────────────────────────────────────────────────────────────────────
  // Asistente NLP simple (Java nativo — no necesita Python)
  // ─────────────────────────────────────────────────────────────────────────────

  public AsistenciaIaResponse responder(String pregunta) {
    String empresaId = currentUserService.getEmpresaId();
    String p = pregunta.toLowerCase(Locale.ROOT);

    long totalPoliticas = politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(0, 1_000)).getTotalElements();
    long politicasActivas = politicaRepository.findByEmpresaIdAndEstadoAndActivoTrue(empresaId, EstadoPolitica.ACTIVA, PageRequest.of(0, 1_000)).getTotalElements();
    double tasaActivacion = totalPoliticas == 0 ? 0 : (politicasActivas * 100.0) / totalPoliticas;
    long nodosEstimados = politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(0, 1_000))
        .getContent().stream().mapToLong(pol -> pol.getNodoIds() == null ? 0 : pol.getNodoIds().size()).sum();
    long transicionesEstimadas = politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(0, 1_000))
        .getContent().stream().mapToLong(pol -> pol.getTransicionIds() == null ? 0 : pol.getTransicionIds().size()).sum();

    String respuesta = buildRespuesta(p, totalPoliticas, politicasActivas, tasaActivacion, nodosEstimados, transicionesEstimadas);
    Map<String, Object> metricas = new HashMap<>();
    metricas.put("politicasTotales", totalPoliticas);
    metricas.put("politicasActivas", politicasActivas);
    metricas.put("tasaActivacion", Math.round(tasaActivacion * 100.0) / 100.0);
    metricas.put("nodosDefinidos", nodosEstimados);
    metricas.put("transicionesDefinidas", transicionesEstimadas);
    return new AsistenciaIaResponse(respuesta, metricas, java.time.LocalDateTime.now());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Generación de diagrama — delega a Python /ia/generar-diagrama
  // ─────────────────────────────────────────────────────────────────────────────

  public GenerarDiagramaResponse generarDiagramaDesdeTexto(String descripcion) {
    String empresaId = currentUserService.getEmpresaId();

    // Obtener departamentos de la empresa para enviárselos a Python
    List<Map<String, String>> deptos = departamentoRepository
        .findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(0, 200))
        .getContent()
        .stream()
        .map(d -> Map.of("id", d.getId(), "nombre", d.getNombre()))
        .toList();

    Map<String, Object> body = new HashMap<>();
    body.put("prompt", descripcion);
    body.put("descripcion", descripcion);
    body.put("departamentos", deptos);

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = restTemplate.postForObject(
          iaServiceUrl + "/ia/generar-diagrama", body, Map.class);

      if (response == null) throw new RuntimeException("Respuesta vacía del microservicio IA");

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> nodos = (List<Map<String, Object>>) response.getOrDefault("nodos", List.of());
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> transiciones = (List<Map<String, Object>>) response.getOrDefault("transiciones", List.of());

      String advertencia = (String) response.get("advertencia");
      if (advertencia != null) log.warn("IA advierte: {}", advertencia);

      // Usar el XML de Python directamente (incluye swimlanes, BPMNEdge, positions).
      // Solo reconstruir desde cero si Python no envió XML válido.
      String xmlFromPython = (String) response.get("xml");
      String xml = (xmlFromPython != null && !xmlFromPython.isBlank())
          ? xmlFromPython
          : construirBpmnXml(nodos, transiciones);

      List<String> tareas = nodos.stream()
          .filter(n -> "TAREA".equals(n.get("tipo")))
          .map(n -> (String) n.get("nombre"))
          .filter(n -> n != null && !n.isBlank())
          .toList();

      return new GenerarDiagramaResponse(xml, tareas);

    } catch (ResourceAccessException e) {
      throw new RuntimeException("El microservicio de IA no está disponible en " + iaServiceUrl
          + ". Inicie el microservicio con: uvicorn main:app --port 5000");
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Generación de campos de formulario — delega a Python /ia/generar-formulario
  // ─────────────────────────────────────────────────────────────────────────────

  public GenerarFormularioResponse generarFormulario(String descripcion, String nombreNodo) {
    Map<String, Object> body = new HashMap<>();
    body.put("descripcion", descripcion);
    body.put("nombreNodo", nombreNodo != null ? nombreNodo : "");

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = restTemplate.postForObject(
          iaServiceUrl + "/ia/generar-formulario", body, Map.class);

      if (response == null) throw new RuntimeException("Respuesta vacía del microservicio IA");

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> campos = (List<Map<String, Object>>) response.getOrDefault("campos", List.of());

      List<GenerarFormularioResponse.CampoIa> camposIa = campos.stream().map(c -> {
        @SuppressWarnings("unchecked")
        List<String> opciones = (List<String>) c.getOrDefault("opciones", List.of());
        return new GenerarFormularioResponse.CampoIa(
            (String) c.getOrDefault("nombre", "campo"),
            (String) c.getOrDefault("etiqueta", "Campo"),
            (String) c.getOrDefault("tipo", "TEXTO"),
            Boolean.TRUE.equals(c.get("requerido")),
            Boolean.TRUE.equals(c.get("es_campo_prioridad")),
            opciones
        );
      }).toList();

      return new GenerarFormularioResponse(camposIa);

    } catch (ResourceAccessException e) {
      throw new RuntimeException("El microservicio de IA no está disponible en " + iaServiceUrl
          + ". Inicie el microservicio con: uvicorn main:app --port 5000");
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Análisis de cuellos de botella — calcula métricas en Java, envía a Python
  // ─────────────────────────────────────────────────────────────────────────────

  public CuelloBotellaResponse analizarCuellosDeBotella(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();

    List<Nodo> nodos = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);
    List<Tramite> tramites = tramiteRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
        .filter(t -> politicaId.equals(t.getPoliticaId()))
        .toList();

    if (nodos.isEmpty()) {
      return new CuelloBotellaResponse(List.of(), 0,
          "No hay nodos definidos en esta política. Dibuja el diagrama primero.");
    }
    if (tramites.isEmpty()) {
      return new CuelloBotellaResponse(List.of(), 0,
          "No hay trámites para esta política. Crea al menos un trámite para analizar.");
    }

    List<Map<String, Object>> metricas = calcularMetricasPorNodo(nodos, tramites, empresaId);

    if (metricas.isEmpty()) {
      return new CuelloBotellaResponse(List.of(), 0,
          "No hay ejecuciones registradas en los nodos. Inicia y avanza al menos un trámite.");
    }

    // Llamar al microservicio Python
    Map<String, Object> body = new HashMap<>();
    body.put("politicaId", politicaId);
    body.put("metricas", metricas);

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = restTemplate.postForObject(
          iaServiceUrl + "/ia/analizar-politica", body, Map.class);

      if (response == null) throw new RuntimeException("Respuesta vacía del microservicio IA");

      return convertirRespuestaPython(response, metricas);

    } catch (ResourceAccessException e) {
      throw new RuntimeException("El microservicio de IA no está disponible en " + iaServiceUrl
          + ". Inicie el microservicio con: uvicorn main:app --port 5000");
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Helpers internos
  // ─────────────────────────────────────────────────────────────────────────────

  private List<Map<String, Object>> calcularMetricasPorNodo(List<Nodo> nodos, List<Tramite> tramites, String empresaId) {
    List<Map<String, Object>> resultado = new ArrayList<>();
    LocalDateTime ahora = LocalDateTime.now();

    for (Nodo nodo : nodos) {
      // Solo analizar nodos PROCESO (tareas ejecutables por usuarios)
      if (nodo.getTipo() != TipoNodo.PROCESO) continue;

      List<EjecucionNodo> ejecuciones = new ArrayList<>();
      for (Tramite tramite : tramites) {
        ejecucionNodoRepository.findByEmpresaIdAndTramiteIdAndActivoTrue(empresaId, tramite.getId())
            .stream()
            .filter(e -> nodo.getId().equals(e.getNodoId()))
            .forEach(ejecuciones::add);
      }

      if (ejecuciones.isEmpty()) continue;

      List<EjecucionNodo> completadas = ejecuciones.stream()
          .filter(e -> e.getEstado() == EstadoEjecucion.COMPLETADO).toList();
      List<EjecucionNodo> rechazadas = ejecuciones.stream()
          .filter(e -> e.getEstado() == EstadoEjecucion.RECHAZADO).toList();
      List<EjecucionNodo> enProceso = ejecuciones.stream()
          .filter(e -> e.getEstado() == EstadoEjecucion.EN_PROCESO).toList();
      List<EjecucionNodo> pendientes = ejecuciones.stream()
          .filter(e -> e.getEstado() == EstadoEjecucion.PENDIENTE).toList();

      // ── Tiempo histórico (COMPLETADO): duracionMs o finEjecucion - inicioEjecucion ──
      double tiempoHistorico = completadas.stream()
          .mapToDouble(e -> {
            if (e.getDuracionMs() != null && e.getDuracionMs() > 0)
              return e.getDuracionMs() / 60_000.0;
            if (e.getInicioEjecucion() != null && e.getFinEjecucion() != null)
              return Duration.between(e.getInicioEjecucion(), e.getFinEjecucion()).toSeconds() / 60.0;
            return 0.0;
          })
          .filter(t -> t > 0)
          .average().orElse(0);

      // ── Tiempo actual (EN_PROCESO): ahora - inicioEjecucion ──────────────────
      double tiempoActual = enProceso.stream()
          .filter(e -> e.getInicioEjecucion() != null)
          .mapToDouble(e -> Duration.between(e.getInicioEjecucion(), ahora).toSeconds() / 60.0)
          .filter(t -> t > 0)
          .average().orElse(0);

      // Usar el mayor para detectar el peor escenario real
      double tiempoPromedio = Math.max(tiempoHistorico, tiempoActual);
      if (tiempoPromedio <= 0) continue;   // sin datos reales → no incluir en métricas

      long totalTerminados = completadas.size() + rechazadas.size();
      double tasaRechazo = totalTerminados > 0 ? (double) rechazadas.size() / totalTerminados : 0;

      double tiempoEspera = completadas.stream()
          .filter(e -> e.getCreadoEn() != null && e.getInicioEjecucion() != null)
          .mapToLong(e -> Duration.between(e.getCreadoEn(), e.getInicioEjecucion()).toMinutes())
          .average().orElse(0);

      List<Double> duraciones = completadas.stream()
          .mapToDouble(e -> {
            if (e.getDuracionMs() != null && e.getDuracionMs() > 0)
              return e.getDuracionMs() / 60_000.0;
            if (e.getInicioEjecucion() != null && e.getFinEjecucion() != null)
              return Duration.between(e.getInicioEjecucion(), e.getFinEjecucion()).toSeconds() / 60.0;
            return 0.0;
          })
          .filter(t -> t > 0)
          .boxed().toList();

      OptionalDouble avgDur = duraciones.stream().mapToDouble(Double::doubleValue).average();
      double varianza = avgDur.isPresent() ? duraciones.stream()
          .mapToDouble(d -> { double diff = d - avgDur.getAsDouble(); return diff * diff; })
          .average().orElse(0) : 0;

      String nombre = nodo.getNombre() != null && !nodo.getNombre().isBlank()
          ? nodo.getNombre() : "Tarea";

      Map<String, Object> metrica = new HashMap<>();
      metrica.put("nodo_id",                      nodo.getId());
      metrica.put("nombre_nodo",                   nombre);
      metrica.put("tiempo_promedio_minutos",        tiempoPromedio);
      metrica.put("tiempo_historico_minutos",       tiempoHistorico);
      metrica.put("tiempo_actual_minutos",          tiempoActual);
      metrica.put("cantidad_ejecuciones_activas",   enProceso.size());
      metrica.put("cantidad_pendientes",            pendientes.size());
      metrica.put("tasa_rechazo",                   tasaRechazo);
      metrica.put("tiempo_espera_promedio_minutos", tiempoEspera);
      metrica.put("varianza_tiempo",                varianza);
      resultado.add(metrica);
    }
    return resultado;
  }

  @SuppressWarnings("unchecked")
  private CuelloBotellaResponse convertirRespuestaPython(Map<String, Object> response, List<Map<String, Object>> metricas) {
    List<Map<String, Object>> resultados = (List<Map<String, Object>>) response.getOrDefault("resultados", List.of());

    List<CuelloBotellaResponse.NodoCuello> criticos = resultados.stream()
        .filter(r -> Boolean.TRUE.equals(r.get("es_cuello_botella")))
        .sorted(Comparator.comparingDouble(r -> -((Number) r.getOrDefault("probabilidad_cuello", 0)).doubleValue()))
        .map(r -> {
          Map<String, Object> met = (Map<String, Object>) r.getOrDefault("metricas", Map.of());
          String sev = (String) r.getOrDefault("severidad", "MEDIA");
          String nivel = switch (sev) { case "CRITICA", "ALTA" -> "ALTO"; case "MEDIA" -> "MEDIO"; default -> "BAJO"; };
          List<String> sug = (List<String>) r.getOrDefault("sugerencias", List.of());
          String sugPrincipal = sug.isEmpty() ? "Revisar la carga de este nodo." : sug.get(0);
          double tiempo = ((Number) met.getOrDefault("tiempo_promedio_minutos", 0)).doubleValue();
          int activas = ((Number) met.getOrDefault("ejecuciones_activas", 0)).intValue();
          return new CuelloBotellaResponse.NodoCuello(
              (String) r.get("nodo_id"), (String) r.get("nombre_nodo"),
              tiempo, activas, nivel, sugPrincipal);
        })
        .toList();

    double promedioGlobal = metricas.stream()
        .mapToDouble(m -> ((Number) m.getOrDefault("tiempo_promedio_minutos", 0)).doubleValue())
        .average().orElse(0);

    long nCriticos = criticos.stream().filter(n -> "ALTO".equals(n.nivelRiesgo())).count();
    String analisis = String.format(
        "Análisis ML completado (ensemble RF+GB+IsolationForest). "
            + "%d nodo(s) son cuellos de botella. %d con riesgo ALTO. %s",
        criticos.size(), nCriticos,
        nCriticos > 0 ? "Se recomienda intervención inmediata." : "El flujo opera dentro de parámetros normales.");

    return new CuelloBotellaResponse(criticos, promedioGlobal, analisis);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Construye BPMN XML a partir de la respuesta de Python
  // ─────────────────────────────────────────────────────────────────────────────

  private String construirBpmnXml(List<Map<String, Object>> nodos, List<Map<String, Object>> transiciones) {
    StringBuilder process = new StringBuilder();
    StringBuilder di = new StringBuilder();

    // Pre-calcular flows por nodo para las tags incoming/outgoing
    Map<String, List<String>> incoming = new HashMap<>();
    Map<String, List<String>> outgoing = new HashMap<>();
    int flowCounter = 0;
    Map<Map<String, Object>, String> flowIds = new HashMap<>();
    for (Map<String, Object> t : transiciones) {
      String orig = (String) t.get("nodoOrigenTempId");
      String dest = (String) t.get("nodoDestinoTempId");
      if (orig == null || dest == null) continue;
      String fid = "Flow_" + orig + "_" + dest + "_" + flowCounter++;
      flowIds.put(t, fid);
      outgoing.computeIfAbsent(orig, k -> new ArrayList<>()).add(fid);
      incoming.computeIfAbsent(dest, k -> new ArrayList<>()).add(fid);
    }

    // Nodes
    for (Map<String, Object> nodo : nodos) {
      String id = (String) nodo.getOrDefault("tempId", "n_x");
      String tipo = (String) nodo.getOrDefault("tipo", "TAREA");
      String nombre = escapeXml((String) nodo.getOrDefault("nombre", "Nodo"));
      int px = toInt(nodo.getOrDefault("posicion_x", 160));
      int py = toInt(nodo.getOrDefault("posicion_y", 100));

      String tag; int w; int h;
      switch (tipo) {
        case "INICIO"   -> { tag = "startEvent";        w = 36;  h = 36;  }
        case "FIN"      -> { tag = "endEvent";           w = 36;  h = 36;  }
        case "DECISION" -> { tag = "exclusiveGateway";  w = 50;  h = 50;  }
        case "PARALELO" -> { tag = "parallelGateway";   w = 50;  h = 50;  }
        default         -> { tag = "task";              w = 120; h = 80;  }
      }

      process.append("    <bpmn:").append(tag).append(" id=\"").append(id)
          .append("\" name=\"").append(nombre).append("\">\n");
      for (String inc : incoming.getOrDefault(id, List.of()))
        process.append("      <bpmn:incoming>").append(inc).append("</bpmn:incoming>\n");
      for (String out : outgoing.getOrDefault(id, List.of()))
        process.append("      <bpmn:outgoing>").append(out).append("</bpmn:outgoing>\n");
      process.append("    </bpmn:").append(tag).append(">\n");

      di.append("      <bpmndi:BPMNShape id=\"").append(id).append("_di\" bpmnElement=\"").append(id).append("\">\n");
      di.append("        <dc:Bounds x=\"").append(px).append("\" y=\"").append(py)
          .append("\" width=\"").append(w).append("\" height=\"").append(h).append("\" />\n");
      di.append("      </bpmndi:BPMNShape>\n");
    }

    // Build nodo lookup for waypoint calculation
    Map<String, Map<String, Object>> nodoById = new HashMap<>();
    nodos.forEach(n -> nodoById.put((String) n.get("tempId"), n));

    // Sequence flows
    for (Map<String, Object> t : transiciones) {
      String orig = (String) t.get("nodoOrigenTempId");
      String dest = (String) t.get("nodoDestinoTempId");
      String fid = flowIds.get(t);
      if (orig == null || dest == null || fid == null) continue;

      String etiqueta = t.get("etiqueta") != null ? " name=\"" + escapeXml((String) t.get("etiqueta")) + "\"" : "";
      process.append("    <bpmn:sequenceFlow id=\"").append(fid).append("\"")
          .append(etiqueta)
          .append(" sourceRef=\"").append(orig).append("\" targetRef=\"").append(dest).append("\" />\n");

      // Waypoints: center-bottom of source → center-top of dest
      Map<String, Object> srcN = nodoById.get(orig);
      Map<String, Object> dstN = nodoById.get(dest);
      if (srcN != null && dstN != null) {
        String srcTipo = (String) srcN.getOrDefault("tipo", "TAREA");
        String dstTipo = (String) dstN.getOrDefault("tipo", "TAREA");
        int sx = toInt(srcN.getOrDefault("posicion_x", 0));
        int sy = toInt(srcN.getOrDefault("posicion_y", 0));
        int dx = toInt(dstN.getOrDefault("posicion_x", 0));
        int dy = toInt(dstN.getOrDefault("posicion_y", 0));
        int sw = nodeWidth(srcTipo); int sh = nodeHeight(srcTipo);
        int dw = nodeWidth(dstTipo);

        di.append("      <bpmndi:BPMNEdge id=\"").append(fid).append("_di\" bpmnElement=\"").append(fid).append("\">\n");
        di.append("        <di:waypoint x=\"").append(sx + sw / 2).append("\" y=\"").append(sy + sh).append("\" />\n");
        di.append("        <di:waypoint x=\"").append(dx + dw / 2).append("\" y=\"").append(dy).append("\" />\n");
        di.append("      </bpmndi:BPMNEdge>\n");
      }
    }

    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
        + "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"
        + "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"
        + "  xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n"
        + "  id=\"Definitions_ia\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
        + "  <bpmn:process id=\"Process_ia\" isExecutable=\"true\">\n"
        + process
        + "  </bpmn:process>\n"
        + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_ia\">\n"
        + "    <bpmndi:BPMNPlane id=\"BPMNPlane_ia\" bpmnElement=\"Process_ia\">\n"
        + di
        + "    </bpmndi:BPMNPlane>\n"
        + "  </bpmndi:BPMNDiagram>\n"
        + "</bpmn:definitions>";
  }

  private int nodeWidth(String tipo)  { return switch(tipo) { case "TAREA" -> 120; default -> 50; }; }
  private int nodeHeight(String tipo) { return switch(tipo) { case "TAREA" -> 80;  default -> 50; }; }
  private int toInt(Object v) { return v == null ? 0 : ((Number) v).intValue(); }
  private String escapeXml(String s) {
    if (s == null) return "";
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Chat NLP (Java nativo — sin Python)
  // ─────────────────────────────────────────────────────────────────────────────

  private String buildRespuesta(String p, long total, long activas, double tasa, long nodos, long trans) {
    if (p.contains("cuello") || p.contains("bottleneck"))
      return "Usa 'Análisis IA' en el menú para detectar cuellos de botella con ML. "
          + "El sistema calculará tiempos reales y enviará los datos al microservicio Python.";
    if (p.contains("predic") || p.contains("fecha"))
      return "Predicción: con tasa de activación del " + Math.round(tasa) + "%, "
          + "las políticas nuevas se estabilizan en 7-14 días en condiciones normales.";
    if (p.contains("optim") || p.contains("suger"))
      return "Sugerencias: 1) activar políticas en borrador validadas, "
          + "2) asegurar que cada tarea tenga departamento asignado, "
          + "3) revisar transiciones en nodos de decisión.";
    if (p.contains("diagram") || p.contains("generar") || p.contains("ia"))
      return "Para generar un diagrama, ve a 'Crear política', describe el proceso en el campo "
          + "descripción y usa el botón 'Generar diagrama'. El microservicio Python analizará el texto.";
    return "Resumen: " + total + " políticas totales, " + activas + " activas ("
        + Math.round(tasa) + "%), " + nodos + " nodos, " + trans + " transiciones. "
        + "Puedes preguntar por optimización, predicciones o cuellos de botella.";
  }
}
