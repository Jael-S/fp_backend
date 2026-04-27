package com.flowpolicy.formulario.repository;

import com.flowpolicy.formulario.model.Formulario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FormularioRepository extends MongoRepository<Formulario, String> {
  List<Formulario> findByEmpresaIdAndNodoIdAndActivoTrue(String empresaId, String nodoId);
  List<Formulario> findByEmpresaIdAndPoliticaIdAndActivoTrue(String empresaId, String politicaId);
  List<Formulario> findByEmpresaIdAndActivoTrue(String empresaId);
  List<Formulario> findByEmpresaIdAndDepartamentoIdAndActivoTrue(String empresaId, String departamentoId);

  Optional<Formulario> findByIdAndEmpresaIdAndActivoTrue(String id, String empresaId);
}
