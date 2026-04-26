package com.flowpolicy.cobertura.repository;

import com.flowpolicy.cobertura.model.PuntoCobertura;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PuntoCoberturaRepository extends MongoRepository<PuntoCobertura, String> {
  List<PuntoCobertura> findByEmpresaIdAndActivoTrue(String empresaId);
  List<PuntoCobertura> findByEmpresaIdAndDepartamentoIdAndActivoTrue(String empresaId, String departamentoId);
  Optional<PuntoCobertura> findByIdAndEmpresaIdAndActivoTrue(String id, String empresaId);
}
