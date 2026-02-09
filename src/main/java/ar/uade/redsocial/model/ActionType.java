package ar.uade.redsocial.model;

/**
 * Tipos de acciones que se registran en el historial (PILA).
 */
public enum ActionType {
    ADD_CLIENT,
    REQUEST_FOLLOW,
    // Iteración 2/3: SEGUIR_CLIENTE, CONECTAR, etc.
    PROCESS_FOLLOW // Opcional, si quisiéramos registrar el procesado
}
