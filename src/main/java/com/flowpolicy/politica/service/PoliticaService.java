package com.flowpolicy.politica.service;

import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.departamento.model.Departamento;
import com.flowpolicy.departamento.repository.DepartamentoRepository;
import com.flowpolicy.formulario.model.Formulario;
import com.flowpolicy.formulario.repository.FormularioRepository;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.politica.dto.PoliticaRequest;
import com.flowpolicy.politica.dto.PoliticaResponse;
import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.model.Politica;
import com.flowpolicy.politica.repository.PoliticaRepository;
import com.flowpolicy.security.CurrentUserService;
import com.flowpolicy.transicion.model.Transicion;
import com.flowpolicy.transicion.repository.TransicionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoliticaService {

  private final PoliticaRepository politicaRepository;
  private final NodoRepository nodoRepository;
  private final TransicionRepository transicionRepository;
  private final DepartamentoRepository departamentoRepository;
  private final FormularioRepository formularioRepository;
  private final CurrentUserService currentUserService;

  public PoliticaResponse create(PoliticaRequest request) {
    var currentUser = currentUserService.getCurrentUser();
    Politica created = politicaRepository.save(Politica.builder()
        .empresaId(currentUser.getEmpresaId())
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .diagramaJson(request.diagramaJson())
        .nodoIds(new ArrayList<>())
        .transicionIds(new ArrayList<>())
        .version(1)
        .estado(EstadoPolitica.BORRADOR)
        .creadoPor(currentUser.getId())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    log.info("Politica creada id={} empresaId={}", created.getId(), created.getEmpresaId());
    return toResponse(created);
  }

  public PoliticaResponse update(String id, PoliticaRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    if (current.getEstado() == EstadoPolitica.ACTIVA) {
      Politica newVersion = Politica.builder()
          .empresaId(current.getEmpresaId())
          .nombre(request.nombre())
          .descripcion(request.descripcion())
          .diagramaJson(request.diagramaJson())
          .nodoIds(new ArrayList<>(current.getNodoIds() == null ? new ArrayList<>() : current.getNodoIds()))
          .transicionIds(new ArrayList<>(current.getTransicionIds() == null ? new ArrayList<>() : current.getTransicionIds()))
          .version(current.getVersion() + 1)
          .estado(EstadoPolitica.BORRADOR)
          .creadoPor(currentUserService.getCurrentUser().getId())
          .activo(true)
          .creadoEn(LocalDateTime.now())
          .build();
      Politica created = politicaRepository.save(newVersion);
      log.info("Politica versionada idOrigen={} nuevaId={} version={}", id, created.getId(), created.getVersion());
      return toResponse(created);
    }

    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setDiagramaJson(request.diagramaJson());
    Politica updated = politicaRepository.save(current);
    log.info("Politica actualizada id={} empresaId={}", updated.getId(), empresaId);
    return toResponse(updated);
  }

  public PoliticaResponse activate(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    validateCanActivate(current);
    current.setEstado(EstadoPolitica.ACTIVA);
    Politica updated = politicaRepository.save(current);
    log.info("Politica activada id={} empresaId={}", updated.getId(), empresaId);
    return toResponse(updated);
  }

  public PoliticaResponse deactivate(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    current.setEstado(EstadoPolitica.BORRADOR);
    return toResponse(politicaRepository.save(current));
  }

  public PoliticaResponse getById(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    return toResponse(current);
  }

  public void delete(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Politica current = politicaRepository.findById(id)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    current.setActivo(false);
    politicaRepository.save(current);
  }

  public PageResponse<PoliticaResponse> list(int page, int size, EstadoPolitica estado) {
    String empresaId = currentUserService.getEmpresaId();
    Page<Politica> result = (estado == null)
        ? politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(page, size))
        : politicaRepository.findByEmpresaIdAndEstadoAndActivoTrue(empresaId, estado, PageRequest.of(page, size));
    return new PageResponse<>(
        result.map(this::toResponse).getContent(),
        result.getTotalElements(),
        result.getNumber(),
        result.getSize()
    );
  }

  public List<PoliticaResponse> listActivas() {
    String empresaId = currentUserService.getEmpresaId();
    return politicaRepository.findByEmpresaIdAndEstadoAndActivoTrue(empresaId, EstadoPolitica.ACTIVA)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private void validateCanActivate(Politica politica) {
    List<Nodo> nodos = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politica.getId(), politica.getEmpresaId());
    if (nodos.isEmpty()) throw new IllegalArgumentException("La politica debe tener nodos para activarse");

    long totalInicio = nodos.stream().filter(n -> n.getTipo() == TipoNodo.INICIO).count();
    long totalFin = nodos.stream().filter(n -> n.getTipo() == TipoNodo.FIN).count();
    long totalTarea = nodos.stream().filter(n -> n.getTipo() == TipoNodo.PROCESO).count();
    if (totalInicio != 1) throw new IllegalArgumentException("El diagrama debe tener un nodo Inicio");
    if (totalFin < 1) throw new IllegalArgumentException("El diagrama debe tener al menos un nodo Fin");
    if (totalTarea < 1) throw new IllegalArgumentException("El diagrama debe tener al menos una Tarea");
    for (Nodo nodo : nodos) {
      if (nodo.getTipo() == TipoNodo.PROCESO && (nodo.getDepartamentoId() == null || nodo.getDepartamentoId().isBlank())) {
        throw new IllegalArgumentException("La tarea '" + nodo.getNombre() + "' no tiene departamento asignado");
      }
    }
  }

  public void ensureBelongsToEmpresa(String politicaId, String empresaId) {
    politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
  }

  public void addNodo(String politicaId, String nodoId) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    if (politica.getNodoIds() == null) {
      politica.setNodoIds(new ArrayList<>());
    }
    if (!politica.getNodoIds().contains(nodoId)) {
      politica.getNodoIds().add(nodoId);
      politicaRepository.save(politica);
    }
  }

  public void addTransicion(String politicaId, String transicionId) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));
    if (politica.getTransicionIds() == null) {
      politica.setTransicionIds(new ArrayList<>());
    }
    if (!politica.getTransicionIds().contains(transicionId)) {
      politica.getTransicionIds().add(transicionId);
      politicaRepository.save(politica);
    }
  }

  public Map<String, Object> saveDiagrama(String politicaId, Map<String, Object> payload) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    String diagramaXml = String.valueOf(payload.getOrDefault("diagramaXml", ""));
    politica.setDiagramaJson(diagramaXml);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nodosPayload = (List<Map<String, Object>>) payload.getOrDefault("nodos", List.of());
    List<String> nodoIds = new ArrayList<>();

    for (Map<String, Object> item : nodosPayload) {
      String nodoId = stringValue(item.get("id"));
      Nodo nodo = (nodoId == null || nodoId.isBlank())
          ? new Nodo()
          : nodoRepository.findByIdAndEmpresaIdAndActivoTrue(nodoId, empresaId).orElse(new Nodo());
      if (nodo.getId() == null) {
        nodo.setPoliticaId(politicaId);
        nodo.setEmpresaId(empresaId);
        nodo.setActivo(true);
        nodo.setCreadoEn(LocalDateTime.now());
      }
      nodo.setNombre(stringValue(item.get("texto")));
      nodo.setDescripcion(stringValue(item.get("texto")));
      nodo.setTipo(parseTipo(stringValue(item.get("tipo"))));
      nodo.setCarril(stringValue(item.get("carril")));
      nodo.setDepartamentoId(stringValue(item.get("departamentoId")));
      nodo.setFormularioId(stringValue(item.get("formularioId")));
      nodo.setPrioridad(stringValue(item.get("prioridad")));
      nodo.setTiempoEstimado(intValue(item.get("tiempoEstimado")));
      Nodo saved = nodoRepository.save(nodo);
      nodoIds.add(saved.getId());
    }

    politica.setNodoIds(nodoIds);
    politicaRepository.save(politica);
    return getDiagrama(politicaId);
  }

  public Map<String, Object> getDiagrama(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    List<Nodo> nodos = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);
    List<Map<String, Object>> serializedNodos = new ArrayList<>();
    for (Nodo nodo : nodos) {
      Map<String, Object> n = new LinkedHashMap<>();
      n.put("id", nodo.getId());
      n.put("tipo", nodo.getTipo() == null ? null : nodo.getTipo().name());
      n.put("texto", nodo.getNombre());
      n.put("carril", nodo.getCarril());
      n.put("departamentoId", nodo.getDepartamentoId());
      n.put("formularioId", nodo.getFormularioId());
      n.put("prioridad", nodo.getPrioridad());
      n.put("tiempoEstimado", nodo.getTiempoEstimado());
      serializedNodos.add(n);
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("diagramaXml", politica.getDiagramaJson());
    out.put("nodos", serializedNodos);
    return out;
  }

  public Map<String, Object> guardarDiagramaConRelaciones(String politicaId, String xml) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    Document doc = parseXml(xml);
    ParsedData parsed = extractDataFromBpmn(doc, politicaId, empresaId);

    List<Nodo> actuales = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);
    for (Nodo nodo : actuales) {
      nodo.setActivo(false);
    }
    nodoRepository.saveAll(actuales);

    List<Transicion> transicionesActuales = transicionRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);
    for (Transicion transicion : transicionesActuales) {
      transicion.setActivo(false);
    }
    transicionRepository.saveAll(transicionesActuales);

    List<Nodo> nodosGuardados = new ArrayList<>();
    for (ParsedNode item : parsed.nodes) {
      Nodo nodo = Nodo.builder()
          .politicaId(politicaId)
          .empresaId(empresaId)
          .nombre(item.name)
          .descripcion(item.name)
          .tipo(item.tipo)
          .elementId(item.elementId)
          .carrilId(item.carrilId)
          .carril(item.carrilNombre)
          .departamentoId(item.departamentoId)
          .formularioId(null)
          .prioridad("MEDIA")
          .tiempoEstimado(24)
          .activo(true)
          .creadoEn(LocalDateTime.now())
          .condiciones(new ArrayList<>())
          .build();
      nodosGuardados.add(nodoRepository.save(nodo));
    }

    Map<String, String> nodoIdByElement = new LinkedHashMap<>();
    for (Nodo nodo : nodosGuardados) {
      nodoIdByElement.put(nodo.getElementId(), nodo.getId());
    }

    List<Transicion> transicionesGuardadas = new ArrayList<>();
    for (ParsedTransition item : parsed.transitions) {
      Transicion transicion = Transicion.builder()
          .politicaId(politicaId)
          .empresaId(empresaId)
          .nodoOrigenId(nodoIdByElement.get(item.origenElementId))
          .nodoDestinoId(nodoIdByElement.get(item.destinoElementId))
          .nombre(item.condicion == null || item.condicion.isBlank() ? "Transicion" : item.condicion)
          .descripcion(null)
          .condicion(item.condicion)
          .requiereAprobacion(false)
          .activo(true)
          .creadoEn(LocalDateTime.now())
          .build();
      transicionesGuardadas.add(transicionRepository.save(transicion));
    }

    politica.setDiagramaJson(xml);
    politica.setNodoIds(nodosGuardados.stream().map(Nodo::getId).toList());
    politica.setTransicionIds(transicionesGuardadas.stream().map(Transicion::getId).toList());
    politicaRepository.save(politica);

    return obtenerDiagramaCompleto(politicaId);
  }

  public void asignarFormularioATarea(String politicaId, String taskId, String formularioId) {
    String empresaId = currentUserService.getEmpresaId();
    ensureBelongsToEmpresa(politicaId, empresaId);

    Nodo nodo = nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(politicaId, empresaId, taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada en la politica"));
    if (nodo.getTipo() != TipoNodo.PROCESO) {
      throw new IllegalArgumentException("Solo se puede asignar formulario a tareas");
    }
    Formulario formulario = formularioRepository.findByIdAndEmpresaIdAndActivoTrue(formularioId, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    nodo.setFormularioId(formulario.getId());
    nodoRepository.save(nodo);
  }

  public void guardarCondicionesDecision(String politicaId, String gatewayId, Map<String, Object> payload) {
    String empresaId = currentUserService.getEmpresaId();
    ensureBelongsToEmpresa(politicaId, empresaId);

    Nodo gateway = nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(politicaId, empresaId, gatewayId)
        .orElseThrow(() -> new ResourceNotFoundException("Decision no encontrada en la politica"));
    if (gateway.getTipo() != TipoNodo.DECISION) {
      throw new IllegalArgumentException("El elemento indicado no es una decision");
    }

    String texto = stringValue(payload.get("texto"));
    if (texto != null && !texto.isBlank()) {
      gateway.setNombre(texto);
      gateway.setDescripcion(texto);
    }

    @SuppressWarnings("unchecked")
    List<Map<String, String>> condiciones = (List<Map<String, String>>) payload.getOrDefault("condiciones", List.of());
    gateway.setCondiciones(condiciones);
    nodoRepository.save(gateway);

    List<Transicion> transiciones = transicionRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);
    for (Transicion transicion : transiciones) {
      if (gateway.getId().equals(transicion.getNodoOrigenId())) {
        Optional<Map<String, String>> match = condiciones.stream()
            .filter(item -> {
              String destino = item.get("destinoId");
              return destino != null && destino.equals(transicion.getNodoDestinoId());
            })
            .findFirst();
        match.ifPresent(item -> transicion.setCondicion(item.get("salida")));
      }
    }
    transicionRepository.saveAll(transiciones);
  }

  public Map<String, Object> obtenerDiagramaCompleto(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    Politica politica = politicaRepository.findById(politicaId)
        .filter(value -> empresaId.equals(value.getEmpresaId()) && value.isActivo())
        .orElseThrow(() -> new ResourceNotFoundException("Politica no encontrada"));

    List<Nodo> nodos = nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);
    List<Transicion> transiciones = transicionRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId);

    List<Map<String, Object>> nodosPayload = nodos.stream().map(n -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", n.getId());
      m.put("elementId", n.getElementId());
      m.put("tipo", n.getTipo() == null ? null : n.getTipo().name());
      m.put("texto", n.getNombre());
      m.put("carrilId", n.getCarrilId());
      m.put("carril", n.getCarril());
      m.put("departamentoId", n.getDepartamentoId());
      m.put("formularioId", n.getFormularioId());
      m.put("condiciones", n.getCondiciones() == null ? List.of() : n.getCondiciones());
      return m;
    }).toList();

    List<Map<String, Object>> transicionesPayload = transiciones.stream().map(t -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", t.getId());
      m.put("origenId", t.getNodoOrigenId());
      m.put("destinoId", t.getNodoDestinoId());
      m.put("condicion", t.getCondicion());
      return m;
    }).toList();

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("politicaId", politica.getId());
    out.put("diagramaXml", politica.getDiagramaJson());
    out.put("nodos", nodosPayload);
    out.put("transiciones", transicionesPayload);
    return out;
  }

  private Document parseXml(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    } catch (Exception ex) {
      throw new IllegalArgumentException("No se pudo parsear el XML BPMN");
    }
  }

  private ParsedData extractDataFromBpmn(Document doc, String politicaId, String empresaId) {
    Map<String, CarrilInfo> processInfoById = new LinkedHashMap<>();
    NodeList participants = doc.getElementsByTagNameNS("*", "participant");
    for (int i = 0; i < participants.getLength(); i++) {
      Element participant = (Element) participants.item(i);
      String participantId = participant.getAttribute("id");
      String participantName = participant.getAttribute("name");
      String processRef = participant.getAttribute("processRef");
      if (participantName == null || participantName.isBlank()) {
        throw new IllegalArgumentException("Cada carril (Pool) debe tener un nombre");
      }
      Departamento departamento = departamentoRepository.findByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(empresaId, participantName)
          .orElseThrow(() -> new IllegalArgumentException("El departamento '" + participantName + "' no existe. Cree el departamento primero."));
      processInfoById.put(processRef, new CarrilInfo(participantId, participantName, departamento.getId()));
    }

    Set<String> taskTags = new HashSet<>(Set.of(
        "task", "userTask", "serviceTask", "manualTask", "scriptTask", "businessRuleTask", "sendTask", "receiveTask"
    ));
    Set<String> gatewayTags = new HashSet<>(Set.of("exclusiveGateway", "parallelGateway", "inclusiveGateway"));

    List<ParsedNode> nodes = new ArrayList<>();
    List<ParsedTransition> transitions = new ArrayList<>();

    NodeList processes = doc.getElementsByTagNameNS("*", "process");
    for (int i = 0; i < processes.getLength(); i++) {
      Element process = (Element) processes.item(i);
      CarrilInfo carril = processInfoById.get(process.getAttribute("id"));

      NodeList childNodes = process.getChildNodes();
      for (int j = 0; j < childNodes.getLength(); j++) {
        if (!(childNodes.item(j) instanceof Element child)) {
          continue;
        }
        String local = child.getLocalName();
        if (local == null) continue;

        if ("startEvent".equals(local)) {
          nodes.add(new ParsedNode(child.getAttribute("id"), safeName(child, "Inicio"), TipoNodo.INICIO, carril));
        } else if ("endEvent".equals(local)) {
          nodes.add(new ParsedNode(child.getAttribute("id"), safeName(child, "Fin"), TipoNodo.FIN, carril));
        } else if (taskTags.contains(local)) {
          nodes.add(new ParsedNode(child.getAttribute("id"), safeName(child, "Tarea"), TipoNodo.PROCESO, carril));
        } else if (gatewayTags.contains(local)) {
          nodes.add(new ParsedNode(child.getAttribute("id"), safeName(child, "Decision"), TipoNodo.DECISION, carril));
        } else if ("sequenceFlow".equals(local)) {
          transitions.add(new ParsedTransition(
              child.getAttribute("sourceRef"),
              child.getAttribute("targetRef"),
              child.getAttribute("name")
          ));
        }
      }
    }

    if (nodes.stream().noneMatch(n -> n.tipo == TipoNodo.INICIO)) {
      throw new IllegalArgumentException("El diagrama debe tener un nodo Inicio");
    }
    if (nodes.stream().noneMatch(n -> n.tipo == TipoNodo.FIN)) {
      throw new IllegalArgumentException("El diagrama debe tener al menos un nodo Fin");
    }

    return new ParsedData(nodes, transitions);
  }

  private String safeName(Element element, String fallback) {
    String raw = element.getAttribute("name");
    return raw == null || raw.isBlank() ? fallback : raw;
  }

  private record CarrilInfo(String id, String nombre, String departamentoId) {}

  private static class ParsedNode {
    final String elementId;
    final String name;
    final TipoNodo tipo;
    final String carrilId;
    final String carrilNombre;
    final String departamentoId;

    ParsedNode(String elementId, String name, TipoNodo tipo, CarrilInfo carril) {
      this.elementId = elementId;
      this.name = name;
      this.tipo = tipo;
      this.carrilId = carril == null ? null : carril.id();
      this.carrilNombre = carril == null ? null : carril.nombre();
      this.departamentoId = carril == null ? null : carril.departamentoId();
    }
  }

  private record ParsedTransition(String origenElementId, String destinoElementId, String condicion) {}

  private record ParsedData(List<ParsedNode> nodes, List<ParsedTransition> transitions) {}

  private String stringValue(Object raw) {
    return raw == null ? null : String.valueOf(raw);
  }

  private Integer intValue(Object raw) {
    if (raw == null) return null;
    try {
      return Integer.parseInt(String.valueOf(raw));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private TipoNodo parseTipo(String value) {
    if (value == null) return TipoNodo.PROCESO;
    if ("TAREA".equalsIgnoreCase(value) || "TASK".equalsIgnoreCase(value)) return TipoNodo.PROCESO;
    try {
      return TipoNodo.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return TipoNodo.PROCESO;
    }
  }

  private PoliticaResponse toResponse(Politica politica) {
    return new PoliticaResponse(
        politica.getId(),
        politica.getEmpresaId(),
        politica.getNombre(),
        politica.getDescripcion(),
        politica.getVersion(),
        politica.getEstado().name(),
        politica.getCreadoPor(),
        politica.getDiagramaJson()
    );
  }
}
