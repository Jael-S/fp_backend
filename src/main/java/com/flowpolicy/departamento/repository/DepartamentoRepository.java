package com.flowpolicy.departamento.repository;

import com.flowpolicy.departamento.model.Departamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DepartamentoRepository extends MongoRepository<Departamento, String> {
  Page<Departamento> findByEmpresaIdAndActivoTrue(String empresaId, Pageable pageable);

  boolean existsByEmpresaIdAndResponsableIdAndActivoTrue(String empresaId, String responsableId);

  Optional<Departamento> findByEmpresaIdAndResponsableIdAndActivoTrue(String empresaId, String responsableId);
}
