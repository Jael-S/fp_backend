package com.flowpolicy.politica.repository;

import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.model.Politica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PoliticaRepository extends MongoRepository<Politica, String> {
  Page<Politica> findByEmpresaIdAndActivoTrue(String empresaId, Pageable pageable);

  Page<Politica> findByEmpresaIdAndEstadoAndActivoTrue(String empresaId, EstadoPolitica estado, Pageable pageable);
}
