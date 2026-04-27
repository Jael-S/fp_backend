package com.flowpolicy.tramite.repository;

import com.flowpolicy.tramite.model.EstadoTramite;
import com.flowpolicy.tramite.model.Tramite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TramiteRepository extends MongoRepository<Tramite, String> {
  Page<Tramite> findByEmpresaIdAndActivoTrue(String empresaId, Pageable pageable);
  Page<Tramite> findByEmpresaIdAndEstadoAndActivoTrue(String empresaId, EstadoTramite estado, Pageable pageable);
  Page<Tramite> findByEmpresaIdAndPoliticaIdAndActivoTrue(String empresaId, String politicaId, Pageable pageable);
  Page<Tramite> findByEmpresaIdAndDepartamentoIdAndActivoTrue(String empresaId, String departamentoId, Pageable pageable);
  Page<Tramite> findByEmpresaIdAndCreadoEnBetweenAndActivoTrue(String empresaId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
  Page<Tramite> findByEmpresaIdAndUsuarioCreadorIdAndActivoTrue(String empresaId, String usuarioId, Pageable pageable);
  List<Tramite> findByEmpresaIdAndActivoTrue(String empresaId);
  List<Tramite> findByEmpresaIdAndDepartamentoIdAndActivoTrue(String empresaId, String departamentoId);
  Optional<Tramite> findByIdAndEmpresaIdAndActivoTrue(String id, String empresaId);
  Optional<Tramite> findByEmpresaIdAndCodigoSeguimientoAndActivoTrue(String empresaId, String codigoSeguimiento);
  Optional<Tramite> findByCodigoSeguimientoAndActivoTrue(String codigoSeguimiento);
}
