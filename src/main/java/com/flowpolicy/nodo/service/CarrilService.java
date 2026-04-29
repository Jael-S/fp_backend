package com.flowpolicy.nodo.service;

import com.flowpolicy.common.exception.ResourceNotFoundException;
import com.flowpolicy.nodo.dto.GuardarRelacionCarrilRequest;
import com.flowpolicy.nodo.dto.RelacionCarrilResponse;
import com.flowpolicy.nodo.model.CarrilDepartamento;
import com.flowpolicy.nodo.repository.CarrilDepartamentoRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrilService {

  private final CarrilDepartamentoRepository carrilDepartamentoRepository;
  private final CurrentUserService currentUserService;

  /**
   * Guardar o actualizar la relación carril-departamento
   */
  public RelacionCarrilResponse guardarRelacion(String politicaId, GuardarRelacionCarrilRequest request) {
    String empresaId = currentUserService.getEmpresaId();

    // Buscar si ya existe
    CarrilDepartamento existing = carrilDepartamentoRepository
        .findByPoliticaIdAndCarrilIdAndActivoTrue(politicaId, request.getCarrilId())
        .orElse(null);

    if (existing != null) {
      // Actualizar
      existing.setDepartamentoId(request.getDepartamentoId());
      existing.setDepartamentoNombre(request.getDepartamentoNombre());
      existing.setActualizadoEn(LocalDateTime.now());
      existing = carrilDepartamentoRepository.save(existing);
      log.info("Relación carril-departamento actualizada: carril={}, departamento={}", 
          request.getCarrilId(), request.getDepartamentoId());
    } else {
      // Crear nuevo
      CarrilDepartamento nuev = CarrilDepartamento.builder()
          .politicaId(politicaId)
          .empresaId(empresaId)
          .carrilId(request.getCarrilId())
          .carrilNombre(request.getCarrilNombre())
          .departamentoId(request.getDepartamentoId())
          .departamentoNombre(request.getDepartamentoNombre())
          .creadoEn(LocalDateTime.now())
          .actualizadoEn(LocalDateTime.now())
          .activo(true)
          .build();
      existing = carrilDepartamentoRepository.save(nuev);
      log.info("Relación carril-departamento creada: carril={}, departamento={}", 
          request.getCarrilId(), request.getDepartamentoId());
    }

    return toResponse(existing);
  }

  /**
   * Obtener relación carril-departamento por carril ID
   */
  public RelacionCarrilResponse obtenerRelacion(String carrilId) {
    CarrilDepartamento relacion = carrilDepartamentoRepository
        .findByCarrilIdAndActivoTrue(carrilId)
        .orElseThrow(() -> new ResourceNotFoundException("Relación carril-departamento no encontrada"));
    return toResponse(relacion);
  }

  /**
   * Obtener relación por política y carril
   */
  public RelacionCarrilResponse obtenerRelacionPorPolitica(String politicaId, String carrilId) {
    CarrilDepartamento relacion = carrilDepartamentoRepository
        .findByPoliticaIdAndCarrilIdAndActivoTrue(politicaId, carrilId)
        .orElseThrow(() -> new ResourceNotFoundException("Relación carril-departamento no encontrada"));
    return toResponse(relacion);
  }

  private RelacionCarrilResponse toResponse(CarrilDepartamento item) {
    return new RelacionCarrilResponse(
        item.getCarrilId(),
        item.getCarrilNombre(),
        item.getDepartamentoId(),
        item.getDepartamentoNombre()
    );
  }
}
