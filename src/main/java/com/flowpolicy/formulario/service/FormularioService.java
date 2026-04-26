package com.flowpolicy.formulario.service;

import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.formulario.dto.CampoResponse;
import com.flowpolicy.formulario.dto.FormularioRequest;
import com.flowpolicy.formulario.dto.FormularioResponse;
import com.flowpolicy.formulario.model.Campo;
import com.flowpolicy.formulario.model.Formulario;
import com.flowpolicy.formulario.repository.FormularioRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormularioService {

  private final FormularioRepository formularioRepository;
  private final CurrentUserService currentUserService;

  public FormularioResponse create(FormularioRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Formulario created = formularioRepository.save(Formulario.builder()
        .empresaId(empresaId)
        .politicaId(request.politicaId())
        .nodoId(request.nodoId())
        .nombre(request.nombre())
        .campos(mapCampos(request))
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    log.info("Formulario creado id={} empresaId={}", created.getId(), empresaId);
    return toResponse(created);
  }

  public List<FormularioResponse> findByNodoId(String nodoId) {
    String empresaId = currentUserService.getEmpresaId();
    return formularioRepository.findByEmpresaIdAndNodoIdAndActivoTrue(empresaId, nodoId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public FormularioResponse update(String id, FormularioRequest request) {
    String empresaId = currentUserService.getEmpresaId();
    Formulario current = formularioRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    current.setPoliticaId(request.politicaId());
    current.setNodoId(request.nodoId());
    current.setNombre(request.nombre());
    current.setCampos(mapCampos(request));
    Formulario updated = formularioRepository.save(current);
    log.info("Formulario actualizado id={} empresaId={}", id, empresaId);
    return toResponse(updated);
  }

  public void deactivate(String id) {
    String empresaId = currentUserService.getEmpresaId();
    Formulario current = formularioRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    current.setActivo(false);
    formularioRepository.save(current);
    log.info("Formulario desactivado id={} empresaId={}", id, empresaId);
  }

  private List<Campo> mapCampos(FormularioRequest request) {
    return request.campos().stream()
        .map(item -> Campo.builder()
            .nombre(item.nombre())
            .etiqueta(item.etiqueta())
            .tipo(item.tipo())
            .requerido(item.requerido())
            .opciones(item.opciones())
            .build())
        .toList();
  }

  private FormularioResponse toResponse(Formulario item) {
    return new FormularioResponse(
        item.getId(),
        item.getPoliticaId(),
        item.getNodoId(),
        item.getNombre(),
        item.getCampos().stream()
            .map(campo -> new CampoResponse(
                campo.getNombre(),
                campo.getEtiqueta(),
                campo.getTipo().name(),
                campo.isRequerido(),
                campo.getOpciones()
            )).toList()
    );
  }
}
