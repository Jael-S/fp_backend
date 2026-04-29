package com.flowpolicy.nodo.dto;

/**
 * Configuración persistida de un nodo BPMN identificado por {@code elementId}.
 */
public record NodoPorElementoResponse(
    String id,
    String elementId,
    String politicaId,
    String nombre,
    String departamentoId,
    String formularioId
) {
}
