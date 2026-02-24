package ar.uade.redsocial.estructuras;

import ar.uade.redsocial.model.Cliente;

import java.util.*;

/**
 * Árbol Binario de Búsqueda ordenado por scoring.
 * 
 * Operaciones principales:
 * - insertar(Cliente): O(log n) promedio, O(n) peor caso
 * - obtenerNivel(int): O(n) - BFS hasta nivel específico
 * 
 * Característica especial para Iteración 2:
 * - Obtener clientes del nivel 4 para ver "quién tiene más seguidores"
 * (aunque el nivel en ABB no tiene relación directa con seguidores,
 * cumplimos el requisito del enunciado)
 */
public class ArbolBinarioBusqueda {

    private NodoABB raiz;
    private int size;

    public ArbolBinarioBusqueda() {
        this.raiz = null;
        this.size = 0;
    }

    /**
     * Inserta un cliente en el árbol según su scoring.
     * Si ya existe un nodo con ese scoring, agrega el cliente a la lista del nodo.
     * 
     * @param cliente el cliente a insertar
     * @complexity O(log n) promedio, O(n) peor caso (árbol desbalanceado)
     */
    public void insertar(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente no puede ser null");
        }

        raiz = insertarRecursivo(raiz, cliente);
        size++;
    }

    private NodoABB insertarRecursivo(NodoABB nodo, Cliente cliente) {
        if (nodo == null) {
            return new NodoABB(cliente);
        }

        int scoringCliente = cliente.getScoring();
        int scoringNodo = nodo.getScoring();

        if (scoringCliente < scoringNodo) {
            nodo.setIzquierdo(insertarRecursivo(nodo.getIzquierdo(), cliente));
        } else if (scoringCliente > scoringNodo) {
            nodo.setDerecho(insertarRecursivo(nodo.getDerecho(), cliente));
        } else {
            // Mismo scoring: agregamos al nodo existente
            nodo.agregarCliente(cliente);
        }

        return nodo;
    }

    /**
     * Obtiene todos los clientes que están en un nivel específico del árbol.
     * Nivel 0 = raíz, nivel 1 = hijos de raíz, etc.
     * 
     * Usa BFS (búsqueda por niveles) para obtener el nivel exacto.
     * 
     * @param nivel el nivel a obtener (0-indexed)
     * @return lista de clientes en ese nivel (puede estar vacía)
     * @complexity O(n) peor caso - debe recorrer hasta ese nivel
     */
    public List<Cliente> obtenerNivel(int nivel) {
        List<Cliente> resultado = new ArrayList<>();

        if (raiz == null || nivel < 0) {
            return resultado;
        }

        // BFS con Queue para recorrer por niveles
        Queue<NodoABB> cola = new LinkedList<>();
        cola.offer(raiz);

        int nivelActual = 0;

        while (!cola.isEmpty() && nivelActual <= nivel) {
            int nodosEnNivel = cola.size();

            // Procesar todos los nodos del nivel actual
            for (int i = 0; i < nodosEnNivel; i++) {
                NodoABB nodoActual = cola.poll();

                if (nivelActual == nivel) {
                    // Estamos en el nivel buscado: agregamos todos los clientes de este nodo
                    resultado.addAll(nodoActual.getClientes());
                } else {
                    // No es el nivel buscado: agregamos hijos a la cola para siguiente nivel
                    if (nodoActual.tieneIzquierdo()) {
                        cola.offer(nodoActual.getIzquierdo());
                    }
                    if (nodoActual.tieneDerecho()) {
                        cola.offer(nodoActual.getDerecho());
                    }
                }
            }

            nivelActual++;
        }

        return resultado;
    }

    /**
     * Wrapper para obtener clientes del nivel 4.
     * Según enunciado: "nivel 4 para ver quién tiene más seguidores"
     * 
     * @return lista de clientes en nivel 4 (0-indexed), ordenados por
     *         followersCount descendente
     * @complexity O(n) + O(k log k) siendo k = clientes en nivel 4
     */
    public List<Cliente> obtenerNivel4() {
        List<Cliente> clientesNivel4 = obtenerNivel(4);

        // Ordenar por followersCount descendente
        clientesNivel4.sort((c1, c2) -> Integer.compare(c2.getFollowersCount(), c1.getFollowersCount()));

        return clientesNivel4;
    }

    /**
     * Retorna todos los clientes del árbol en orden inorder (menor a mayor
     * scoring).
     * Útil para debugging y tests.
     * 
     * @return lista de clientes ordenados por scoring
     * @complexity O(n)
     */
    public List<Cliente> inorder() {
        List<Cliente> resultado = new ArrayList<>();
        inorderRecursivo(raiz, resultado);
        return resultado;
    }

    private void inorderRecursivo(NodoABB nodo, List<Cliente> resultado) {
        if (nodo == null) {
            return;
        }

        inorderRecursivo(nodo.getIzquierdo(), resultado);
        resultado.addAll(nodo.getClientes());
        inorderRecursivo(nodo.getDerecho(), resultado);
    }

    /**
     * Retorna la altura del árbol.
     * 
     * @return altura (raíz tiene altura 0, árbol vacío tiene altura -1)
     * @complexity O(n)
     */
    public int altura() {
        return alturaRecursiva(raiz);
    }

    private int alturaRecursiva(NodoABB nodo) {
        if (nodo == null) {
            return -1;
        }

        int alturaIzq = alturaRecursiva(nodo.getIzquierdo());
        int alturaDer = alturaRecursiva(nodo.getDerecho());

        return 1 + Math.max(alturaIzq, alturaDer);
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    public int size() {
        return size;
    }

    public NodoABB getRaiz() {
        return raiz;
    }
}
