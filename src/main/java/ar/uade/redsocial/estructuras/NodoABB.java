package ar.uade.redsocial.estructuras;

import ar.uade.redsocial.model.Cliente;
import java.util.ArrayList;
import java.util.List;

/**
 * Nodo del Árbol Binario de Búsqueda.
 * 
 * Ordenamiento por scoring (clave primaria).
 * Criterio de desempate: nombre lexicográfico (para mantener determinismo).
 * 
 * Invariantes:
 * - cliente != null
 * - clientes con mismo scoring se almacenan en lista
 */
public class NodoABB {

    private final int scoring; // clave de ordenamiento
    private final List<Cliente> clientes; // clientes con este scoring
    private NodoABB izquierdo;
    private NodoABB derecho;

    public NodoABB(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente no puede ser null");
        }
        this.scoring = cliente.getScoring();
        this.clientes = new ArrayList<>();
        this.clientes.add(cliente);
        this.izquierdo = null;
        this.derecho = null;
    }

    public int getScoring() {
        return scoring;
    }

    public List<Cliente> getClientes() {
        return new ArrayList<>(clientes); // copia defensiva
    }

    public void agregarCliente(Cliente cliente) {
        if (cliente.getScoring() != this.scoring) {
            throw new IllegalArgumentException(
                    "Cliente con scoring " + cliente.getScoring() +
                            " no puede agregarse a nodo con scoring " + this.scoring);
        }
        clientes.add(cliente);
    }

    public NodoABB getIzquierdo() {
        return izquierdo;
    }

    public void setIzquierdo(NodoABB izquierdo) {
        this.izquierdo = izquierdo;
    }

    public NodoABB getDerecho() {
        return derecho;
    }

    public void setDerecho(NodoABB derecho) {
        this.derecho = derecho;
    }

    public boolean tieneIzquierdo() {
        return izquierdo != null;
    }

    public boolean tieneDerecho() {
        return derecho != null;
    }

    public boolean esHoja() {
        return !tieneIzquierdo() && !tieneDerecho();
    }
}
