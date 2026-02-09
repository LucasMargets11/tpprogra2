package ar.uade.redsocial.model;

import java.time.LocalDateTime;

/**
 * Solicitud de seguimiento que se procesa en orden FIFO (COLA).
 */
public record FollowRequest(String solicitante, String objetivo, LocalDateTime fechaHora) { }
