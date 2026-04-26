package com.flowpolicy.politica.service;

import com.flowpolicy.common.dto.PageResponse;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.politica.dto.PoliticaRequest;
import com.flowpolicy.politica.dto.PoliticaResponse;
import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.model.Politica;
import com.flowpolicy.politica.repository.PoliticaRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoliticaService {

  private final PoliticaRepository politicaRepository;
  private final NodoRepository nodoRepository;
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
