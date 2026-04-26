package com.flowpolicy.formulario.repository;

import com.flowpolicy.formulario.model.Formulario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FormularioRepository extends MongoRepository<Formulario, String> {
  List<Formulario> findByEmpresaIdAndNodoIdAndActivoTrue(String empresaId, String nodoId);

  Optional<Formulario> findByIdAndEmpresaIdAndActivoTrue(String id, String empresaId);
}
