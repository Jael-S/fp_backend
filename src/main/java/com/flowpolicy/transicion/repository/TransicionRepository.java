package com.flowpolicy.transicion.repository;

import com.flowpolicy.transicion.model.Transicion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TransicionRepository extends MongoRepository<Transicion, String> {
  List<Transicion> findByPoliticaIdAndEmpresaIdAndActivoTrue(String politicaId, String empresaId);

  List<Transicion> findByNodoOrigenIdAndEmpresaIdAndActivoTrue(String nodoOrigenId, String empresaId);

  List<Transicion> findByPoliticaIdAndNodoOrigenIdAndEmpresaIdAndActivoTrue(
      String politicaId, String nodoOrigenId, String empresaId);

  Optional<Transicion> findByIdAndEmpresaIdAndActivoTrue(String id, String empresaId);
}
