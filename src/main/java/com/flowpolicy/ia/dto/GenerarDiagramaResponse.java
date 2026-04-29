package com.flowpolicy.ia.dto;

import java.util.List;

public record GenerarDiagramaResponse(
    String diagramaXml,
    List<String> tareasDetectadas
) {}
