package com.flowpolicy.security;

import com.flowpolicy.auth.model.Usuario;
import com.flowpolicy.auth.repository.UsuarioRepository;
import com.flowpolicy.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

  private final UsuarioRepository usuarioRepository;

  public Usuario getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      throw new ResourceNotFoundException("No se encontro usuario autenticado");
    }

    return usuarioRepository.findByEmail(authentication.getName())
        .orElseThrow(() -> new ResourceNotFoundException("No se encontro usuario autenticado"));
  }

  public String getEmpresaId() {
    return getCurrentUser().getEmpresaId();
  }
}
