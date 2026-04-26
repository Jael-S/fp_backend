package com.flowpolicy.auth.repository;

import com.flowpolicy.auth.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.flowpolicy.auth.model.Rol;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
  Optional<Usuario> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByEmailAndEmpresaId(String email, String empresaId);

  Page<Usuario> findByEmpresaIdAndActivoTrue(String empresaId, Pageable pageable);

  Page<Usuario> findByEmpresaIdAndDepartamentoIdAndActivoTrue(String empresaId, String departamentoId, Pageable pageable);

  Page<Usuario> findByEmpresaIdAndRolAndActivoTrue(String empresaId, Rol rol, Pageable pageable);

  Page<Usuario> findByEmpresaIdAndActivoTrueAndNombreContainingIgnoreCase(String empresaId, String nombre, Pageable pageable);

  Page<Usuario> findByEmpresaIdAndDepartamentoIdAndRolAndActivoTrue(String empresaId, String departamentoId, Rol rol, Pageable pageable);

  List<Usuario> findByDepartamentoId(String departamentoId);

  long countByDepartamentoId(String departamentoId);

  long countByEmpresaIdAndDepartamentoIdAndActivoTrue(String empresaId, String departamentoId);
}

