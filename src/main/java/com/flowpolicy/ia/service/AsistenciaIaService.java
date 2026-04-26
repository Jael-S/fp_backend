package com.flowpolicy.ia.service;

import com.flowpolicy.ia.dto.AsistenciaIaResponse;
import com.flowpolicy.politica.model.EstadoPolitica;
import com.flowpolicy.politica.repository.PoliticaRepository;
import com.flowpolicy.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AsistenciaIaService {

  private final CurrentUserService currentUserService;
  private final PoliticaRepository politicaRepository;

  public AsistenciaIaResponse responder(String pregunta) {
    String empresaId = currentUserService.getEmpresaId();
    String preguntaNormalizada = pregunta.toLowerCase(Locale.ROOT);

    long totalPoliticas = politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(0, 1_000)).getTotalElements();
    long politicasActivas = politicaRepository.findByEmpresaIdAndEstadoAndActivoTrue(empresaId, EstadoPolitica.ACTIVA, PageRequest.of(0, 1_000)).getTotalElements();
    double tasaActivacion = totalPoliticas == 0 ? 0 : (politicasActivas * 100.0) / totalPoliticas;

    long nodosEstimados = politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(0, 1_000))
        .getContent()
        .stream()
        .mapToLong(p -> p.getNodoIds() == null ? 0 : p.getNodoIds().size())
        .sum();

    long transicionesEstimadas = politicaRepository.findByEmpresaIdAndActivoTrue(empresaId, PageRequest.of(0, 1_000))
        .getContent()
        .stream()
        .mapToLong(p -> p.getTransicionIds() == null ? 0 : p.getTransicionIds().size())
        .sum();

    String respuesta = buildRespuesta(preguntaNormalizada, totalPoliticas, politicasActivas, tasaActivacion, nodosEstimados, transicionesEstimadas);
    Map<String, Object> metricas = new HashMap<>();
    metricas.put("politicasTotales", totalPoliticas);
    metricas.put("politicasActivas", politicasActivas);
    metricas.put("tasaActivacionPoliticas", Math.round(tasaActivacion * 100.0) / 100.0);
    metricas.put("nodosDefinidos", nodosEstimados);
    metricas.put("transicionesDefinidas", transicionesEstimadas);
    metricas.put("timestamp", LocalDateTime.now());

    return new AsistenciaIaResponse(respuesta, metricas, LocalDateTime.now());
  }

  private String buildRespuesta(
      String pregunta,
      long totalPoliticas,
      long politicasActivas,
      double tasaActivacion,
      long nodosEstimados,
      long transicionesEstimadas
  ) {
    if (pregunta.contains("cuello") || pregunta.contains("bottleneck")) {
      return "Cuello de botella detectado: hay " + nodosEstimados + " nodos para " + transicionesEstimadas
          + " transiciones. Recomendacion: revisar politicas con nodos de decision sin transiciones suficientes.";
    }

    if (pregunta.contains("predic") || pregunta.contains("fecha")) {
      return "Prediccion estimada: con una tasa de activacion de " + Math.round(tasaActivacion) + "%, "
          + "las politicas nuevas podrian estabilizarse en 7 a 14 dias si mantienen la relacion actual nodo/transicion.";
    }

    if (pregunta.contains("optim") || pregunta.contains("suger")) {
      return "Sugerencias: 1) aumentar cobertura de transiciones por nodo, 2) activar politicas en borrador validadas, "
          + "3) reducir nodos redundantes en politicas con baja activacion.";
    }

    if (pregunta.contains("tiempo") || pregunta.contains("promedio")) {
      return "Aun no hay modulo de ejecucion historica en este backend. Como proxy operativo, hay "
          + totalPoliticas + " politicas y " + nodosEstimados + " nodos definidos para modelar tiempos por etapa.";
    }

    return "Resumen operativo: " + totalPoliticas + " politicas totales, " + politicasActivas
        + " activas (" + Math.round(tasaActivacion) + "%). Puedes preguntar por cuellos de botella, predicciones u optimizacion.";
  }
}
