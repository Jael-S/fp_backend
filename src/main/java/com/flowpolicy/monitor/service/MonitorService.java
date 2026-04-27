package com.flowpolicy.monitor.service;

import com.flowpolicy.tramite.service.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonitorService {

  private final TramiteService tramiteService;

  public Map<String, Object> estadoPolitica(String politicaId) {
    return tramiteService.monitorByPolitica(politicaId);
  }
}
