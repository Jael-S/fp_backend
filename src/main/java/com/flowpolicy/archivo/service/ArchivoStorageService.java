package com.flowpolicy.archivo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ArchivoStorageService {

  private final Path basePath;

  public ArchivoStorageService(@Value("${flowpolicy.upload.path:uploads}") String uploadPath) {
    this.basePath = Path.of(uploadPath).toAbsolutePath();
  }

  public String save(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    try {
      Files.createDirectories(basePath);
      String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
      Path target = basePath.resolve(filename);
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      return target.toString();
    } catch (IOException e) {
      throw new RuntimeException("No se pudo guardar archivo adjunto", e);
    }
  }
}
