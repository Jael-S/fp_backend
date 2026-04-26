package com.flowpolicy.nodo.repository;

import com.flowpolicy.nodo.model.Nodo;
import com.flowpolicy.nodo.model.TipoNodo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NodoRepository extends MongoRepository<Nodo, String> {
  List<Nodo> findByPoliticaIdAndEmpresaIdAndActivoTrue(String politicaId, String empresaId);

  long countByPoliticaIdAndEmpresaIdAndActivoTrueAndTipo(String politicaId, String empresaId, TipoNodo tipo);

  Optional<Nodo> findByIdAndEmpresaIdAndActivoTrue(String id, String empresaId);
}
