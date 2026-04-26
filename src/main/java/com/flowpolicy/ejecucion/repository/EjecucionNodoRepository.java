package com.flowpolicy.ejecucion.repository;

import com.flowpolicy.ejecucion.model.EjecucionNodo;
import com.flowpolicy.ejecucion.model.EstadoEjecucion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EjecucionNodoRepository extends MongoRepository<EjecucionNodo, String> {
  List<EjecucionNodo> findByEmpresaIdAndTramiteIdAndActivoTrue(String empresaId, String tramiteId);
  List<EjecucionNodo> findByEmpresaIdAndUsuarioAsignadoIdAndEstadoAndActivoTrue(String empresaId, String usuarioAsignadoId, EstadoEjecucion estado);
  List<EjecucionNodo> findByEmpresaIdAndUsuarioAsignadoIdAndActivoTrue(String empresaId, String usuarioAsignadoId);
  Optional<EjecucionNodo> findByIdAndEmpresaIdAndActivoTrue(String id, String empresaId);
}
