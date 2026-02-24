package ar.uade.redsocial.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TAD Cliente
 * Invariantes:
 * - nombre != null && !nombre.isBlank()
 * - scoring >= 0
 * - siguiendo.size() <= 2 (máximo 2 seguimientos)
 * - followersCount >= 0
 */
public class Cliente {

    private static final int MAX_SEGUIMIENTOS = 2;

    private final String nombre;
    private final int scoring;

    // Preparado para iteraciones futuras
    private final Set<String> siguiendo = new HashSet<>();
    private final Set<String> conexiones = new HashSet<>();

    // Iteración 2: contador de seguidores
    private int followersCount = 0;

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

    public int getFollowersCount() {
        return followersCount;
    }

    // Iteración 2: validaciones de seguimiento
    public void seguirA(String nombreCliente) {
        // Validación 1: No seguirse a sí mismo
        if (this.nombre.equals(nombreCliente)) {
            throw new IllegalArgumentException("Un cliente no puede seguirse a sí mismo");
        }

        // Validación 2: No duplicar seguimiento
        if (siguiendo.contains(nombreCliente)) {
            throw new IllegalArgumentException("Ya está siguiendo a " + nombreCliente);
        }

        // Validación 3: Máximo 2 seguimientos
        if (siguiendo.size() >= MAX_SEGUIMIENTOS) {
            throw new IllegalStateException(
                    "No se puede seguir a más de " + MAX_SEGUIMIENTOS + " clientes");
        }

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

    // Iteración 2: gestión de followersCount
    public void incrementarFollowers() {
        followersCount++;
    }

    public void decrementarFollowers() {
        if (followersCount > 0) {
            followersCount--;
        }
    }
}
