package com.flowpolicy.auth.repository;

import com.flowpolicy.auth.model.Empresa;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmpresaRepository extends MongoRepository<Empresa, String> {
  List<Empresa> findByActivoTrue();
}
