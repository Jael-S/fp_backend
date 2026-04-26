package com.flowpolicy.empresa.service;

import com.flowpolicy.auth.model.Empresa;
import com.flowpolicy.auth.repository.EmpresaRepository;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.empresa.dto.EmpresaRequest;
import com.flowpolicy.empresa.dto.EmpresaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaService {

  private final EmpresaRepository empresaRepository;

  public EmpresaResponse create(EmpresaRequest request) {
    Empresa created = empresaRepository.save(Empresa.builder()
        .nombre(request.nombre())
        .descripcion(request.descripcion())
        .email(request.email())
        .telefono(request.telefono())
        .activo(true)
        .creadoEn(LocalDateTime.now())
        .build());
    return toResponse(created);
  }

  public List<EmpresaResponse> list() {
    return empresaRepository.findByActivoTrue().stream().map(this::toResponse).toList();
  }

  public EmpresaResponse getById(String id) {
    return toResponse(empresaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
  }

  public EmpresaResponse update(String id, EmpresaRequest request) {
    Empresa current = empresaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
    current.setNombre(request.nombre());
    current.setDescripcion(request.descripcion());
    current.setEmail(request.email());
    current.setTelefono(request.telefono());
    return toResponse(empresaRepository.save(current));
  }

  public void deactivate(String id) {
    Empresa current = empresaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
    current.setActivo(false);
    empresaRepository.save(current);
  }

  private EmpresaResponse toResponse(Empresa empresa) {
    return new EmpresaResponse(
        empresa.getId(),
        empresa.getNombre(),
        empresa.getDescripcion(),
        empresa.getEmail(),
        empresa.getTelefono(),
        empresa.isActivo()
    );
  }
}
