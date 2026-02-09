package ar.uade.redsocial.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TAD Cliente
 * Invariantes:
 * - nombre != null && !nombre.isBlank()
 * - scoring >= 0
 */
public class Cliente {

    private final String nombre;
    private final int scoring;

    // Preparado para iteraciones futuras
    private final Set<String> siguiendo = new HashSet<>();
    private final Set<String> conexiones = new HashSet<>();

    public Cliente(String nombre, int scoring) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Nombre inválido");
        }
        if (scoring < 0) {
            throw new IllegalArgumentException("Scoring inválido");
        }
        this.nombre = nombre;
        this.scoring = scoring;
    }

    public String getNombre() {
        return nombre;
    }

    public int getScoring() {
        return scoring;
    }

    public Set<String> getSiguiendo() {
        return Collections.unmodifiableSet(siguiendo);
    }

    public Set<String> getConexiones() {
        return Collections.unmodifiableSet(conexiones);
    }

    // Métodos para iteración 2 / 3
    public void seguirA(String nombreCliente) {
        siguiendo.add(nombreCliente);
    }

    public void agregarConexion(String nombreCliente) {
        conexiones.add(nombreCliente);
    }

    // Métodos necesarios para mantener consistencia en Undo (Iteración 1)
    public void dejarDeSeguir(String nombreCliente) {
        siguiendo.remove(nombreCliente);
    }

    public void removerConexion(String nombreCliente) {
        conexiones.remove(nombreCliente);
    }
}
