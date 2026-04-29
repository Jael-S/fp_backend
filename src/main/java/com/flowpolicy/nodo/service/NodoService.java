package com.flowpolicy.nodo.service;

import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.departamento.model.Departamento;
import com.flowpolicy.departamento.repository.DepartamentoRepository;
import com.flowpolicy.formulario.model.Formulario;
import com.flowpolicy.formulario.repository.FormularioRepository;
import com.flowpolicy.nodo.dto.NodoPorElementoResponse;
import com.flowpolicy.nodo.dto.NodoPosicionRequest;
import com.flowpolicy.nodo.dto.NodoRequest;
import com.flowpolicy.nodo.dto.NodoResponse;
import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import com.flowpolicy.nodo.repository.NodoRepository;
import com.flowpolicy.politica.service.PoliticaService;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodoService {

  private final NodoRepository nodoRepository;
  private final CurrentUserService currentUserService;
  private final PoliticaService politicaService;
  private final FormularioRepository formularioRepository;
  private final DepartamentoRepository departamentoRepository;

  public NodoResponse create(String politicaId, NodoRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    validateInicioConstraint(null, politicaId, empresaId, request.tipo());
    Nodo created = nodoRepository.save(Nodo.builder()
        .politicaId(politicaId)
        .empresaId(empresaId)
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .tipo(request.tipo())
        .formularioId(request.formularioId())
        .posicionX(request.posicionX())
        .posicionY(request.posicionY())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    politicaService.addNodo(politicaId, created.getId());
    log.info("Nodo creado id={} politicaId={}", created.getId(), politicaId);
    return toResponse(created);
  }

  public List<NodoResponse> listByPolitica(String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    return nodoRepository.findByPoliticaIdAndEmpresaIdAndActivoTrue(politicaId, empresaId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public Optional<NodoPorElementoResponse> findByElementIdAndPoliticaId(String elementId, String politicaId) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    return nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(politicaId, empresaId, elementId)
        .map(this::toPorElementoResponse);
  }

  public NodoResponse update(String id, NodoRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    validateInicioConstraint(current, empresaId, request.tipo());
    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setTipo(request.tipo());
    current.setFormularioId(request.formularioId());
    current.setPosicionX(request.posicionX());
    current.setPosicionY(request.posicionY());
    return toResponse(nodoRepository.save(current));
  }

  public NodoResponse updatePosition(String id, NodoPosicionRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    current.setPosicionX(request.posicionX());
    current.setPosicionY(request.posicionY());
    return toResponse(nodoRepository.save(current));
  }

  public void delete(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    current.setActivo(false);
    nodoRepository.save(current);
  }

  public Map<String, String> getFormularioByNodoId(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo current = nodoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
    return Map.of(
        "nodoId", current.getId(),
        "formularioId", current.getFormularioId() == null ? "" : current.getFormularioId()
    );
  }

  public Formulario obtenerFormularioPorNodo(String politicaId, String nodoElementId) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    Nodo nodo = nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(politicaId, empresaId, nodoElementId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado en politica"));
    if (nodo.getFormularioId() == null || nodo.getFormularioId().isBlank()) {
      throw new ResourceNotFoundException("El nodo no tiene formulario asignado");
    }
    return formularioRepository.findByIdAndEmpresaIdAndActivoTrue(nodo.getFormularioId(), empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
  }

  public Departamento obtenerDepartamentoPorNodo(String politicaId, String nodoElementId) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
    Nodo nodo = nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(politicaId, empresaId, nodoElementId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado en politica"));
    if (nodo.getDepartamentoId() == null || nodo.getDepartamentoId().isBlank()) {
      throw new ResourceNotFoundException("El nodo no tiene departamento asignado");
    }
    return departamentoRepository.findById(nodo.getDepartamentoId())
        .filter(Departamento::isActivo)
        .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado"));
  }

  private NodoResponse toResponse(Nodo item) {
    return new NodoResponse(
        item.getId(),
        item.getPoliticaId(),
        item.getNombre(),
        item.getDescripcion(),
        item.getTipo().name(),
        item.getFormularioId(),
        item.getPosicionX(),
        item.getPosicionY(),
        item.getTipoFlujo()
    );
  }

  private NodoPorElementoResponse toPorElementoResponse(Nodo n) {
    return new NodoPorElementoResponse(
        n.getId(),
        n.getElementId(),
        n.getPoliticaId(),
        n.getNombre(),
        n.getDepartamentoId(),
        n.getFormularioId()
    );
  }

  private void validateInicioConstraint(String nodoId, String politicaId, String empresaId, com.flowpolicy.nodo.model.TipoNodo tipo) {
    if (tipo != com.flowpolicy.nodo.model.TipoNodo.INICIO) {
      return;
    }
    long totalInicio = nodoRepository.countByPoliticaIdAndEmpresaIdAndActivoTrueAndTipo(
        politicaId,
        empresaId,
        com.flowpolicy.nodo.model.TipoNodo.INICIO
    );
    if (totalInicio > 0 && nodoId == null) {
      throw new IllegalArgumentException("Solo se permite un nodo INICIO por politica");
    }
  }

  /**
   * Asignar un formulario a un nodo
   */
  public NodoResponse asignarFormulario(String nodoId, String formularioId) {
    String empresaId = currentUserService.getEmpresaId();
    Nodo nodo = getNodoOrThrow(nodoId, empresaId);
    
    // Validar que el formulario existe
    if (formularioId != null && !formularioId.isEmpty()) {
      formularioRepository.findById(formularioId)
          .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    }
    
    nodo.setFormularioId(formularioId);
    Nodo updated = nodoRepository.save(nodo);
    log.info("Formulario {} asignado a nodo {}", formularioId, nodoId);
    return toResponse(updated);
  }

  /**
   * Actualizar el tipo de flujo de una decisión.
   * Si {@code politicaId} no es nulo, {@code nodoIdOrElementId} es el elementId BPMN (ej. Gateway_xxx).
   */
  public NodoResponse actualizarTipoFlujo(String nodoIdOrElementId, String tipoFlujo, String politicaId) {
    if (tipoFlujo == null || tipoFlujo.isBlank()) {
      throw new IllegalArgumentException("tipoFlujo es requerido");
    }
    String empresaId = currentUserService.getEmpresaId();
    Nodo nodo;
    if (politicaId != null && !politicaId.isBlank()) {
      politicaService.ensureBelongsToEmpresa(politicaId, empresaId);
      nodo = nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(politicaId, empresaId, nodoIdOrElementId)
          .orElseGet(() -> {
            Nodo created = nodoRepository.save(Nodo.builder()
                .politicaId(politicaId)
                .empresaId(empresaId)
                .elementId(nodoIdOrElementId)
                .tipo(TipoNodo.DECISION)
                .nombre("Decisión")
                .descripcion("Decisión")
                .activo(true)
                .creadoEn(LocalDateTime.now())
                .condiciones(new java.util.ArrayList<>())
                .build());
            politicaService.addNodo(politicaId, created.getId());
            return created;
          });
      if (nodo.getTipo() != TipoNodo.DECISION) {
        nodo.setTipo(TipoNodo.DECISION);
      }
    } else {
      nodo = getNodoOrThrow(nodoIdOrElementId, empresaId);
      if (nodo.getTipo() != TipoNodo.DECISION) {
        throw new IllegalArgumentException("Solo se puede actualizar tipo de flujo en nodos de decisión");
      }
    }

    nodo.setTipoFlujo(tipoFlujo);

    if (nodo.getCondiciones() == null) {
      nodo.setCondiciones(new java.util.ArrayList<>());
    }
    nodo.getCondiciones().removeIf(m -> m != null && "TIPO_FLUJO".equals(m.get("tipo")));
    Map<String, String> tipoMap = new java.util.LinkedHashMap<>();
    tipoMap.put("tipo", "TIPO_FLUJO");
    tipoMap.put("valor", tipoFlujo == null ? "" : tipoFlujo);
    nodo.getCondiciones().add(tipoMap);

    Nodo updated = nodoRepository.save(nodo);
    log.info("Tipo de flujo {} actualizado para nodo id={} elementId={}", tipoFlujo, updated.getId(), updated.getElementId());
    return toResponse(updated);
  }

  private Nodo getNodoOrThrow(String nodoId, String empresaId) {
    return nodoRepository.findByIdAndEmpresaId(nodoId, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado"));
  }

  private void validateInicioConstraint(Nodo current, String empresaId, com.flowpolicy.nodo.model.TipoNodo nuevoTipo) {
    if (nuevoTipo != com.flowpolicy.nodo.model.TipoNodo.INICIO) {
      return;
    }
    long totalInicio = nodoRepository.countByPoliticaIdAndEmpresaIdAndActivoTrueAndTipo(
        current.getPoliticaId(),
        empresaId,
        com.flowpolicy.nodo.model.TipoNodo.INICIO
    );
    boolean currentIsInicio = current.getTipo() == com.flowpolicy.nodo.model.TipoNodo.INICIO;
    if (totalInicio > 0 && !currentIsInicio) {
      throw new IllegalArgumentException("Solo se permite un nodo INICIO por politica");
    }
  }

  /**
   * Asignar formulario a una tarea (por elementId del nodo BPMN)
   * Busca o crea el nodo si no existe
   */
  public void asignarFormularioATarea(String politicaId, String nodoElementId, String formularioId) {
    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);

    // Buscar nodo por elementId
    Nodo nodo = nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(politicaId, empresaId, nodoElementId)
        .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado en política"));

    // Validar que el formulario existe
    if (formularioId != null && !formularioId.isEmpty()) {
      formularioRepository.findById(formularioId)
          .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    }

    nodo.setFormularioId(formularioId);
    nodoRepository.save(nodo);
    log.info("Formulario {} asignado a tarea con elementId {}", formularioId, nodoElementId);
  }

  /**
   * Upsert nodo por elementId BPMN: nombre, departamento y formulario (sin depender del carril).
   */
  public NodoResponse configurarPorElementId(String nodoElementId, Map<String, String> body) {
    String politicaId = body.get("politicaId");
    if (politicaId == null || politicaId.isBlank()) {
      throw new IllegalArgumentException("politicaId es requerido");
    }

    String nombre = blankToNull(body.get("nombre"));
    String departamentoId = blankToNull(body.get("departamentoId"));
    String formularioId = blankToNull(body.get("formularioId"));

    String empresaId = currentUserService.getEmpresaId();
    politicaService.ensureBelongsToEmpresa(politicaId, empresaId);

    if (formularioId != null && departamentoId == null) {
      throw new IllegalArgumentException("departamentoId es requerido cuando se asigna un formulario");
    }

    if (departamentoId != null) {
      Departamento dept = departamentoRepository.findById(departamentoId)
          .filter(Departamento::isActivo)
          .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado"));
      if (!empresaId.equals(dept.getEmpresaId())) {
        throw new IllegalArgumentException("El departamento no pertenece a la empresa");
      }
    }

    if (formularioId != null) {
      Formulario form = formularioRepository.findByIdAndEmpresaIdAndActivoTrue(formularioId, empresaId)
          .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
      if (form.getDepartamentoId() != null && !form.getDepartamentoId().isBlank()
          && departamentoId != null && !form.getDepartamentoId().equals(departamentoId)) {
        throw new IllegalArgumentException("El formulario no pertenece al departamento seleccionado");
      }
    }

    Optional<Nodo> existing = nodoRepository.findByPoliticaIdAndEmpresaIdAndElementIdAndActivoTrue(
        politicaId, empresaId, nodoElementId);

    Nodo nodo;
    if (existing.isPresent()) {
      nodo = existing.get();
    } else {
      nodo = nodoRepository.save(Nodo.builder()
          .politicaId(politicaId)
          .empresaId(empresaId)
          .elementId(nodoElementId)
          .tipo(TipoNodo.PROCESO)
          .activo(true)
          .creadoEn(LocalDateTime.now())
          .build());
      politicaService.addNodo(politicaId, nodo.getId());
    }

    nodo.setNombre(nombre);
    nodo.setDepartamentoId(departamentoId);
    nodo.setFormularioId(formularioId);
    if (nodo.getElementId() == null || nodo.getElementId().isBlank()) {
      nodo.setElementId(nodoElementId);
    }

    Nodo saved = nodoRepository.save(nodo);
    log.info("Nodo configurado elementId={} politicaId={}", nodoElementId, politicaId);
    return toResponse(saved);
  }

  private static String blankToNull(String v) {
    if (v == null || v.isBlank()) {
      return null;
    }
    return v;
  }
}
