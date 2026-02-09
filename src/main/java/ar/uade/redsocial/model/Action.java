package ar.uade.redsocial.model;

import java.time.LocalDateTime;

/**
 * Registro de una acción para poder deshacer (LIFO).
 */
public record Action(ActionType type, String detalle, Object payload, LocalDateTime fechaHora) {

    public Action(ActionType type, String detalle, LocalDateTime fechaHora) {
        this(type, detalle, null, fechaHora);
    }

}
