package com.flowpolicy.publico.repository;

import com.flowpolicy.publico.model.SeguimientoCliente;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SeguimientoClienteRepository extends MongoRepository<SeguimientoCliente, String> {
}
