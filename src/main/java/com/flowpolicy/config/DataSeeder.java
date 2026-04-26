package com.flowpolicy.config;

import com.flowpolicy.auth.model.Rol;
import com.flowpolicy.auth.model.Empresa;
import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.EmpresaRepository;
import com.flowpolicy.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

  private final UsuarioRepository usuarioRepository;
  private final EmpresaRepository empresaRepository;
  private final PasswordEncoder passwordEncoder;
  private final MongoTemplate mongoTemplate;

  @Value("${app.seeder.startup-enabled:false}")
  private boolean startupSeederEnabled;

  @Bean
  public CommandLineRunner initSeedData() {
    return args -> {
      if (!startupSeederEnabled) {
        log.info("DataSeeder deshabilitado por configuracion.");
        return;
      }

      if (usuarioRepository.count() > 0) {
        log.info("La coleccion usuarios ya contiene datos. Ejecutando reparacion de datos legacy.");
        repairLegacyUsers();
        return;
      }

      log.info("Iniciando DataSeeder inicial de usuarios FlowPolicy...");
      seedUsuariosBase();
      log.info("DataSeeder completado correctamente.");
    };
  }

  private void seedUsuariosBase() {
    Empresa empresaBase = empresaRepository.save(
        Empresa.builder()
            .nombre("FlowPolicy Demo")
            .activo(true)
            .creadoEn(LocalDateTime.now())
            .build()
    );

    List<Usuario> usuarios = List.of(
        Usuario.builder()
            .nombre("Admin FlowPolicy")
            .email("admin@flowpolicy.com")
            .passwordHash(passwordEncoder.encode("admin123"))
            .rol(Rol.GESTOR_SISTEMA)
            .empresaId(empresaBase.getId())
            .departamentoId(null)
            .activo(true)
            .creadoEn(LocalDateTime.now())
            .build(),
        Usuario.builder()
            .nombre("Admin Area Demo")
            .email("admin.area@flowpolicy.com")
            .passwordHash(passwordEncoder.encode("admin123"))
            .rol(Rol.ADMINISTRADOR_AREA)
            .empresaId(empresaBase.getId())
            .departamentoId("DEP-001")
            .activo(true)
            .creadoEn(LocalDateTime.now())
            .build(),
        Usuario.builder()
            .nombre("Operador Demo")
            .email("operador@flowpolicy.com")
            .passwordHash(passwordEncoder.encode("admin123"))
            .rol(Rol.FUNCIONARIO)
            .empresaId(empresaBase.getId())
            .departamentoId("DEP-001")
            .activo(true)
            .creadoEn(LocalDateTime.now())
            .build()
    );

    usuarioRepository.saveAll(usuarios);
  }

  private void repairLegacyUsers() {
    Empresa empresaBase = empresaRepository.findAll().stream().findFirst().orElseGet(() ->
        empresaRepository.save(
            Empresa.builder()
                .nombre("FlowPolicy Default")
                .activo(true)
                .creadoEn(LocalDateTime.now())
                .build()
        )
    );

    // Primero se corrigen roles legacy en crudo para evitar errores de parseo de enum.
    long rolesMigrated = mongoTemplate.updateMulti(
        Query.query(Criteria.where("rol").nin(
            Rol.GESTOR_SISTEMA.name(),
            Rol.ADMINISTRADOR_AREA.name(),
            Rol.FUNCIONARIO.name(),
            Rol.OPERADOR.name()
        )),
        Update.update("rol", Rol.FUNCIONARIO.name()),
        "usuarios"
    ).getModifiedCount();

    long empresaFixed = mongoTemplate.updateMulti(
        new Query(new Criteria().orOperator(
            Criteria.where("empresaId").exists(false),
            Criteria.where("empresaId").is(null),
            Criteria.where("empresaId").is("")
        )),
        Update.update("empresaId", empresaBase.getId()),
        "usuarios"
    ).getModifiedCount();

    List<Usuario> users = usuarioRepository.findAll().stream()
        .map(user -> {
          boolean changed = false;
          if (user.getEmpresaId() == null || user.getEmpresaId().isBlank()) {
            user.setEmpresaId(empresaBase.getId());
            changed = true;
          }
          if (user.getRol() != null && user.getRol().normalized() != user.getRol()) {
            user.setRol(user.getRol().normalized());
            changed = true;
          }
          return changed ? user : null;
        })
        .filter(java.util.Objects::nonNull)
        .toList();

    if (!users.isEmpty()) {
      usuarioRepository.saveAll(users);
      log.info("Reparacion legacy completada. Roles migrados={}, empresaId corregidos={}, usuarios actualizados={}",
          rolesMigrated, empresaFixed, users.size());
    } else {
      log.info("Reparacion legacy sin cambios por repositorio. Roles migrados={}, empresaId corregidos={}",
          rolesMigrated, empresaFixed);
    }
  }
}

