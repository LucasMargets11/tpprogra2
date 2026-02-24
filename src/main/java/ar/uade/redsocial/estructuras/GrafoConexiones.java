package ar.uade.redsocial.estructuras;

import java.util.*;

/**
 * TAD GrafoConexiones (Grafo no dirigido, no ponderado)
 * 
 * Representa conexiones bidireccionales entre clientes.
 * 
 * Operaciones principales:
 * - agregarConexion(u, v): O(1) promedio (bidireccional)
 * - vecinos(u): O(1) promedio
 * - calcularDistancia(origen, destino): O(V + E) con BFS
 * 
 * Invariantes:
 * - Simetría: (u, v) ∈ E ⟺ (v, u) ∈ E
 * - Sin auto-loops: (u, u) ∉ E
 * - Sin aristas duplicadas
 */
public class GrafoConexiones {

    // nombre_cliente -> conjunto de vecinos (adyacentes)
    private final Map<String, Set<String>> adyacencias;

    public GrafoConexiones() {
        this.adyacencias = new HashMap<>();
    }

    /**
     * Agrega una conexión bidireccional entre dos clientes.
     * Si la conexión ya existe, no hace nada (idempotente).
     * 
     * @param cliente1 primer cliente
     * @param cliente2 segundo cliente
     * @throws IllegalArgumentException si los nombres son inválidos o iguales
     * @complexity O(1) promedio
     */
    public void agregarConexion(String cliente1, String cliente2) {
        validarNombres(cliente1, cliente2);

        if (cliente1.equals(cliente2)) {
            throw new IllegalArgumentException("No se permiten auto-conexiones");
        }

        // Bidireccional: agregar en ambas direcciones
        adyacencias.computeIfAbsent(cliente1, k -> new HashSet<>()).add(cliente2);
        adyacencias.computeIfAbsent(cliente2, k -> new HashSet<>()).add(cliente1);
    }

    /**
     * Retorna el conjunto de vecinos (clientes conectados) de un cliente.
     * Si el cliente no tiene conexiones, retorna conjunto vacío.
     * 
     * @param cliente el nombre del cliente
     * @return conjunto inmutable de vecinos
     * @complexity O(1) promedio
     */
    public Set<String> vecinos(String cliente) {
        if (cliente == null || cliente.isBlank()) {
            throw new IllegalArgumentException("Nombre de cliente inválido");
        }

        Set<String> vecinos = adyacencias.get(cliente);
        if (vecinos == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(vecinos);
    }

    /**
     * Calcula la distancia en saltos entre dos clientes usando BFS.
     * 
     * @param origen  cliente origen
     * @param destino cliente destino
     * @return distancia en saltos, -1 si no hay camino
     * @throws IllegalArgumentException si algún cliente no existe en el grafo
     * @complexity O(V + E) siendo V = vértices, E = aristas
     */
    public int calcularDistancia(String origen, String destino) {
        validarNombres(origen, destino);

        // Caso especial: mismo cliente
        if (origen.equals(destino)) {
            return 0;
        }

        // Validar que ambos clientes existen en el grafo
        if (!adyacencias.containsKey(origen)) {
            throw new IllegalArgumentException("Cliente origen no existe en el grafo: " + origen);
        }
        if (!adyacencias.containsKey(destino)) {
            throw new IllegalArgumentException("Cliente destino no existe en el grafo: " + destino);
        }

        // BFS para encontrar camino más corto
        Queue<String> cola = new LinkedList<>();
        Map<String, Integer> distancias = new HashMap<>();
        Set<String> visitados = new HashSet<>();

        cola.offer(origen);
        distancias.put(origen, 0);
        visitados.add(origen);

        while (!cola.isEmpty()) {
            String actual = cola.poll();
            int distanciaActual = distancias.get(actual);

            // Explorar vecinos
            for (String vecino : vecinos(actual)) {
                if (!visitados.contains(vecino)) {
                    visitados.add(vecino);
                    distancias.put(vecino, distanciaActual + 1);
                    cola.offer(vecino);

                    // Si encontramos el destino, retornamos inmediatamente
                    if (vecino.equals(destino)) {
                        return distancias.get(vecino);
                    }
                }
            }
        }

        // No hay camino entre origen y destino
        return -1;
    }

    /**
     * Agrega un cliente al grafo sin conexiones.
     * Útil para inicializar clientes antes de agregar conexiones.
     * 
     * @param cliente nombre del cliente
     * @complexity O(1)
     */
    public void agregarCliente(String cliente) {
        if (cliente == null || cliente.isBlank()) {
            throw new IllegalArgumentException("Nombre de cliente inválido");
        }
        adyacencias.putIfAbsent(cliente, new HashSet<>());
    }

    /**
     * Verifica si existe una conexión entre dos clientes.
     * 
     * @param cliente1 primer cliente
     * @param cliente2 segundo cliente
     * @return true si hay conexión directa
     * @complexity O(1) promedio
     */
    public boolean existeConexion(String cliente1, String cliente2) {
        Set<String> vecinos1 = adyacencias.get(cliente1);
        return vecinos1 != null && vecinos1.contains(cliente2);
    }

    /**
     * Retorna la cantidad de clientes en el grafo.
     * 
     * @return cantidad de vértices
     * @complexity O(1)
     */
    public int cantidadClientes() {
        return adyacencias.size();
    }

    /**
     * Retorna la cantidad total de conexiones (aristas).
     * Como el grafo es no dirigido, contamos cada arista una sola vez.
     * 
     * @return cantidad de aristas
     * @complexity O(V) siendo V = cantidad de vértices
     */
    public int cantidadConexiones() {
        int suma = adyacencias.values().stream()
                .mapToInt(Set::size)
                .sum();
        return suma / 2; // Cada arista se cuenta dos veces (bidireccional)
    }

    /**
     * Verifica si el grafo está vacío.
     * 
     * @return true si no hay clientes
     * @complexity O(1)
     */
    public boolean estaVacio() {
        return adyacencias.isEmpty();
    }

    /**
     * Retorna todos los clientes del grafo.
     * 
     * @return conjunto inmutable de nombres de clientes
     * @complexity O(1)
     */
    public Set<String> clientes() {
        return Collections.unmodifiableSet(adyacencias.keySet());
    }

    // Métodos de validación

    private void validarNombres(String cliente1, String cliente2) {
        if (cliente1 == null || cliente1.isBlank()) {
            throw new IllegalArgumentException("Nombre de cliente1 inválido");
        }
        if (cliente2 == null || cliente2.isBlank()) {
            throw new IllegalArgumentException("Nombre de cliente2 inválido");
        }
    }
}
