package com.flowpolicy.nodo.repository;

import com.flowpolicy.nodo.model.CarrilDepartamento;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CarrilDepartamentoRepository extends MongoRepository<CarrilDepartamento, String> {
  
  Optional<CarrilDepartamento> findByPoliticaIdAndCarrilIdAndActivoTrue(
      String politicaId, String carrilId);
  
  Optional<CarrilDepartamento> findByCarrilIdAndActivoTrue(String carrilId);
  
  List<CarrilDepartamento> findByPoliticaIdAndActivoTrue(String politicaId);
  
  List<CarrilDepartamento> findByDepartamentoIdAndActivoTrue(String departamentoId);
}
