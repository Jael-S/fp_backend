package com.flowpolicy.notificacion.repository;

import com.flowpolicy.notificacion.model.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
  Page<Notificacion> findByEmpresaIdAndUsuarioIdOrderByCreadoEnDesc(String empresaId, String usuarioId, Pageable pageable);
  long countByEmpresaIdAndUsuarioIdAndLeidaFalse(String empresaId, String usuarioId);
  Optional<Notificacion> findByIdAndEmpresaIdAndUsuarioId(String id, String empresaId, String usuarioId);
  void deleteByIdAndEmpresaIdAndUsuarioId(String id, String empresaId, String usuarioId);
}
