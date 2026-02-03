package ar.uade.redsocial.model;

import java.time.LocalDateTime;

/**
 * Registro de una acción para poder deshacer (LIFO).
 */
public record Action(ActionType type, String detalle, LocalDateTime fechaHora) { }
