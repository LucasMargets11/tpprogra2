package ar.uade.redsocial.service;

import ar.uade.redsocial.dto.ClienteDTO;
import ar.uade.redsocial.estructuras.ArbolBinarioBusqueda;
import ar.uade.redsocial.estructuras.GrafoConexiones;
import ar.uade.redsocial.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * TAD RedSocialEmpresarial
 *
 * Estructuras utilizadas:
 * - HashMap para búsqueda eficiente por nombre (O(1) promedio)
 * - TreeMap para índice por scoring (O(log n))
 * - ArrayDeque como pila para historial de acciones (LIFO, O(1))
 * - ArrayDeque como cola para solicitudes de seguimiento (FIFO, O(1))
 * - ArbolBinarioBusqueda (ABB propio) para nivel 4 (Iteración 2)
 * - GrafoConexiones para conexiones bidireccionales (Iteración 3)
 * 
 * - Interfaces
 */
public class RedSocialEmpresarial {

    private static final Gson gson = new Gson();

    // nombre -> Cliente
    private final Map<String, Cliente> clientesPorNombre = new HashMap<>();

    // scoring -> conjunto de nombres (ordenado)
    private final NavigableMap<Integer, Set<String>> indicePorScoring = new TreeMap<>();

    // Historial de acciones (PILA)
    private final Deque<Action> historial = new ArrayDeque<>();

    // Solicitudes de seguimiento (COLA)
    private final Deque<FollowRequest> colaSeguimientos = new ArrayDeque<>();

    // Iteración 2: ABB propio para obtener nivel 4
    private final ArbolBinarioBusqueda abb = new ArbolBinarioBusqueda();

    // Iteración 3: Grafo de conexiones bidireccionales
    private final GrafoConexiones grafo = new GrafoConexiones();

    // los seguimientos deben estar en el orden en el que fueron enviados. y que sea
    // individual por cada usuario.
    // Clase auxiliar para mapear la raíz del JSON
    private static class JsonDataWrapper {
        List<ClienteDTO> clientes;
    }

    // ---------------- CARGA DE DATOS ----------------

    public void loadFromJson(String ruta) {
        Path path = Path.of(ruta);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Archivo no encontrado: " + ruta);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonDataWrapper wrapper = gson.fromJson(reader, JsonDataWrapper.class);

            if (wrapper == null || wrapper.clientes == null) {
                // Si el archivo no tiene la estructura {"clientes": ...} o está vacío
                return;
            }

            for (ClienteDTO dto : wrapper.clientes) {
                // Validación estricta de duplicados y scoring
                if (clientesPorNombre.containsKey(dto.nombre)) {
                    throw new IllegalArgumentException("Cliente duplicado en JSON: " + dto.nombre);
                }

                // Validación Iteración 2: máximo 2 seguimientos
                if (dto.siguiendo != null && dto.siguiendo.size() > 2) {
                    throw new IllegalArgumentException(
                            "Cliente '" + dto.nombre + "' tiene " + dto.siguiendo.size() +
                                    " seguimientos (máximo permitido: 2)");
                }

                // addClienteInterno ya valida scoring < 0
                Cliente nuevo = addClienteInterno(dto.nombre, dto.scoring);

                // Parseo tolerante de listas
                if (dto.siguiendo != null) {
                    for (String seguido : dto.siguiendo) {
                        // seguirA() ya valida auto-seguimiento, duplicados y máximo 2
                        try {
                            nuevo.seguirA(seguido);
                        } catch (IllegalArgumentException | IllegalStateException e) {
                            throw new IllegalArgumentException(
                                    "Error al cargar seguimientos de '" + dto.nombre + "': " + e.getMessage());
                        }
                    }
                }
                if (dto.conexiones != null) {
                    for (String conexion : dto.conexiones) {
                        nuevo.agregarConexion(conexion);
                    }
                }
            }

            // Segunda pasada: actualizar followersCount basado en seguimientos cargados
            for (Cliente cliente : clientesPorNombre.values()) {
                for (String nombreSeguido : cliente.getSiguiendo()) {
                    Cliente seguido = clientesPorNombre.get(nombreSeguido);
                    if (seguido != null) {
                        seguido.incrementarFollowers();
                    }
                }
            }

            // Tercera pasada (Iteración 3): cargar conexiones en el grafo
            for (Cliente cliente : clientesPorNombre.values()) {
                for (String nombreConexion : cliente.getConexiones()) {
                    Cliente clienteConexion = clientesPorNombre.get(nombreConexion);
                    if (clienteConexion == null) {
                        // Política: ignorar conexiones a clientes inexistentes con warning
                        System.err.println("WARNING: Cliente '" + cliente.getNombre() +
                                "' tiene conexión a cliente inexistente: " + nombreConexion);
                        continue;
                    }
                    // agregarConexion es idempotente y bidireccional
                    // Se agregará dos veces (una por cada lado) pero solo se guardará una vez
                    grafo.agregarConexion(cliente.getNombre(), nombreConexion);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo JSON", e);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("JSON inválido o mal formateado", e);
        }
    }

    // ---------------- CLIENTES ----------------

    public void agregarCliente(String nombre, int scoring) {
        if (clientesPorNombre.containsKey(nombre)) {
            throw new IllegalArgumentException("Ya existe el cliente: " + nombre);
        }

        Cliente cliente = addClienteInterno(nombre, scoring);

        // Registramos acción
        registrarAccion(new Action(
                ActionType.ADD_CLIENT,
                nombre,
                cliente,
                LocalDateTime.now()));
    }

    // Método interno que NO registra en historial (usado por carga JSON)
    private Cliente addClienteInterno(String nombre, int scoring) {
        validarNombre(nombre);
        validarScoring(scoring);

        Cliente cliente = new Cliente(nombre, scoring);
        clientesPorNombre.put(nombre, cliente);

        indicePorScoring
                .computeIfAbsent(scoring, k -> new HashSet<>())
                .add(nombre);

        // Iteración 2: insertar en ABB
        abb.insertar(cliente);

        // Iteración 3: agregar al grafo (sin conexiones inicialmente)
        grafo.agregarCliente(nombre);

        return cliente;
    }

    public Cliente buscarPorNombre(String nombre) {
        return clientesPorNombre.get(nombre);
    }

    public List<Cliente> buscarPorScoring(int scoring) {
        Set<String> nombres = indicePorScoring.getOrDefault(scoring, Collections.emptySet());
        List<Cliente> resultado = new ArrayList<>();

        for (String nombre : nombres) {
            resultado.add(clientesPorNombre.get(nombre));
        }
        return resultado;
    }

    public List<Cliente> buscarPorScoringEntre(int min, int max) {
        NavigableMap<Integer, Set<String>> subMapa = indicePorScoring.subMap(min, true, max, true);

        List<Cliente> resultado = new ArrayList<>();
        for (Set<String> nombres : subMapa.values()) {
            for (String nombre : nombres) {
                resultado.add(clientesPorNombre.get(nombre));
            }
        }
        return resultado;
    }

    public int cantidadClientes() {
        return clientesPorNombre.size();
    }

    // ---------------- HISTORIAL (PILA) & UNDO ----------------

    private void registrarAccion(Action accion) {
        historial.push(accion);
    }

    /**
     * @deprecated Usar {@link #undo()} para una API más moderna.
     */
    @Deprecated
    public Action deshacerUltimaAccion() {
        return undo().orElse(null);
    }

    public Optional<Action> undo() {
        if (historial.isEmpty()) {
            return Optional.empty();
        }

        Action ultima = historial.pop();

        switch (ultima.type()) {
            case ADD_CLIENT -> eliminarClienteCompleto(ultima.detalle());
            case REQUEST_FOLLOW -> deshacerSolicitudSeguir(ultima);
            default -> throw new IllegalStateException("Acción desconocida en historial: " + ultima.type());
        }

        return Optional.of(ultima);
    }

    private void eliminarClienteCompleto(String nombre) {
        // 1. Eliminar del mapa principal
        Cliente eliminado = clientesPorNombre.remove(nombre);
        if (eliminado == null)
            return; // Ya no existía (raro pero defensivo)

        // 2. Eliminar del índice por scoring
        Set<String> nombresEnScoring = indicePorScoring.get(eliminado.getScoring());
        if (nombresEnScoring != null) {
            nombresEnScoring.remove(nombre);
            if (nombresEnScoring.isEmpty()) {
                indicePorScoring.remove(eliminado.getScoring());
            }
        }

        // 3. Limpiar referencias en otros clientes para evitar inconsistencias
        for (Cliente otro : clientesPorNombre.values()) {
            otro.dejarDeSeguir(nombre);
            otro.removerConexion(nombre);
        }
    }

    private void deshacerSolicitudSeguir(Action action) {
        if (colaSeguimientos.isEmpty()) {
            throw new IllegalStateException("Inconsistencia: Undo REQUEST_FOLLOW pero la cola está vacía.");
        }

        // Recuperar y remover la última solicitud (LIFO behavior sobre la estructura
        // usada como Queue)
        // La solicitud correspondiente a ESTA acción debería ser la última agregada.
        FollowRequest lastRequest = colaSeguimientos.removeLast();

        // Validar integridad
        Object payload = action.payload();
        if (payload instanceof FollowRequest originalRequest) {
            if (!originalRequest.equals(lastRequest)) {
                // Rollback parcial (volvemos a poner lo que sacamos)
                colaSeguimientos.addLast(lastRequest);
                throw new IllegalStateException(
                        "Corrupción de historial: Se intentó deshacer '%s' pero en la cola estaba '%s'"
                                .formatted(originalRequest, lastRequest));
            }
        }
    }

    // ---------------- SEGUIMIENTOS (COLA) ----------------

    public void solicitarSeguir(String solicitante, String objetivo) {
        validarNombre(solicitante);
        validarNombre(objetivo);

        if (!clientesPorNombre.containsKey(solicitante) ||
                !clientesPorNombre.containsKey(objetivo)) {
            throw new IllegalArgumentException("Cliente inexistente: " + solicitante + " o " + objetivo);
        }

        FollowRequest request = new FollowRequest(solicitante, objetivo, LocalDateTime.now());
        colaSeguimientos.addLast(request);

        registrarAccion(new Action(
                ActionType.REQUEST_FOLLOW,
                solicitante + " -> " + objetivo,
                request,
                LocalDateTime.now()));
    }

    public FollowRequest procesarSiguienteSolicitud() {
        return colaSeguimientos.pollFirst(); // FIFO
    }

    /**
     * Iteración 2: Confirma un seguimiento y actualiza el contador de seguidores.
     * 
     * @param solicitante el cliente que quiere seguir
     * @param objetivo    el cliente a seguir
     * @throws IllegalArgumentException si los clientes no existen o las
     *                                  validaciones fallan
     * @throws IllegalStateException    si el solicitante ya sigue a 2 clientes
     */
    public void confirmarSeguimiento(String solicitante, String objetivo) {
        Cliente clienteSolicitante = buscarPorNombre(solicitante);
        Cliente clienteObjetivo = buscarPorNombre(objetivo);

        if (clienteSolicitante == null || clienteObjetivo == null) {
            throw new IllegalArgumentException("Cliente inexistente: " + solicitante + " o " + objetivo);
        }

        // seguirA() ya valida máximo 2, auto-seguimiento y duplicados
        clienteSolicitante.seguirA(objetivo);
        clienteObjetivo.incrementarFollowers();
    }

    public int cantidadSolicitudesPendientes() {
        return colaSeguimientos.size();
    }

    // ---------------- VALIDACIONES ----------------

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Nombre inválido");
        }
    }

    private void validarScoring(int scoring) {
        if (scoring < 0) {
            throw new IllegalArgumentException("Scoring inválido");
        }
    }

    // ---------------- ITERACIÓN 2: ABB Y NIVEL 4 ----------------

    /**
     * Obtiene los clientes del nivel 4 del ABB ordenados por followersCount
     * descendente.
     * El nivel 4 sirve para visualizar "quién tiene más seguidores" según
     * enunciado.
     * 
     * @return lista de clientes en nivel 4 ordenados por cantidad de seguidores
     * @complexity O(n) para BFS + O(k log k) para ordenar, siendo k = clientes en
     *             nivel 4
     */
    public List<Cliente> obtenerClientesNivel4() {
        return abb.obtenerNivel4();
    }

    /**
     * Obtiene el ABB completo (útil para debugging y tests).
     * 
     * @return ABB con todos los clientes insertados
     */
    public ArbolBinarioBusqueda getABB() {
        return abb;
    }

    // ---------------- ITERACIÓN 3: GRAFO Y DISTANCIAS ----------------

    /**
     * Obtiene los vecinos (clientes directamente conectados) de un cliente.
     * 
     * @param nombreCliente nombre del cliente
     * @return conjunto inmutable de nombres de vecinos
     * @complexity O(1) promedio
     */
    public Set<String> obtenerVecinos(String nombreCliente) {
        return grafo.vecinos(nombreCliente);
    }

    /**
     * Calcula la distancia en saltos entre dos clientes usando BFS.
     * 
     * @param origen  nombre del cliente origen
     * @param destino nombre del cliente destino
     * @return distancia en saltos (0 si son el mismo, -1 si no hay camino)
     * @throws IllegalArgumentException si algún cliente no existe
     * @complexity O(V + E) siendo V = vértices, E = aristas
     */
    public int calcularDistancia(String origen, String destino) {
        return grafo.calcularDistancia(origen, destino);
    }

    /**
     * Agrega una conexión bidireccional entre dos clientes.
     * 
     * @param cliente1 primer cliente
     * @param cliente2 segundo cliente
     * @throws IllegalArgumentException si los clientes no existen o son inválidos
     * @complexity O(1) promedio
     */
    public void agregarConexion(String cliente1, String cliente2) {
        if (!clientesPorNombre.containsKey(cliente1)) {
            throw new IllegalArgumentException("Cliente no existe: " + cliente1);
        }
        if (!clientesPorNombre.containsKey(cliente2)) {
            throw new IllegalArgumentException("Cliente no existe: " + cliente2);
        }

        grafo.agregarConexion(cliente1, cliente2);

        // Sincronizar con Cliente (opcional, para consistencia con modelo actual)
        clientesPorNombre.get(cliente1).agregarConexion(cliente2);
        clientesPorNombre.get(cliente2).agregarConexion(cliente1);
    }

    /**
     * Verifica si existe una conexión directa entre dos clientes.
     * 
     * @param cliente1 primer cliente
     * @param cliente2 segundo cliente
     * @return true si hay conexión directa
     * @complexity O(1) promedio
     */
    public boolean existeConexion(String cliente1, String cliente2) {
        return grafo.existeConexion(cliente1, cliente2);
    }

    /**
     * Obtiene el grafo completo (útil para debugging y tests).
     * 
     * @return grafo de conexiones
     */
    public GrafoConexiones getGrafo() {
        return grafo;
    }

    // ---------------- SNAPSHOT (para runner de tests manuales) ----------------

    /**
     * Genera un snapshot completo del estado actual del sistema.
     * Útil para debugging y visualización en el runner interactivo.
     * 
     * @return mapa con toda la información del sistema
     */
    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        // Métricas generales
        snapshot.put("cantidadClientes", clientesPorNombre.size());
        snapshot.put("cantidadSolicitudesPendientes", colaSeguimientos.size());
        snapshot.put("alturaABB", abb.altura());
        snapshot.put("clientesEnGrafo", grafo.cantidadClientes());
        snapshot.put("conexionesEnGrafo", grafo.cantidadConexiones());

        // Lista de clientes con detalles
        List<Map<String, Object>> clientes = new ArrayList<>();
        for (Cliente c : clientesPorNombre.values()) {
            Map<String, Object> clienteData = new LinkedHashMap<>();
            clienteData.put("nombre", c.getNombre());
            clienteData.put("scoring", c.getScoring());
            clienteData.put("followersCount", c.getFollowersCount());
            clienteData.put("siguiendo", new ArrayList<>(c.getSiguiendo()));
            clienteData.put("conexiones", new ArrayList<>(c.getConexiones()));
            clientes.add(clienteData);
        }
        clientes.sort((a, b) -> Integer.compare((int) b.get("scoring"), (int) a.get("scoring")));
        snapshot.put("clientes", clientes);

        // Índice por scoring
        Map<Integer, List<String>> indexScoring = new TreeMap<>();
        for (Map.Entry<Integer, Set<String>> entry : indicePorScoring.entrySet()) {
            indexScoring.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        snapshot.put("indicePorScoring", indexScoring);

        // Solicitudes pendientes
        List<Map<String, String>> solicitudes = new ArrayList<>();
        for (FollowRequest req : colaSeguimientos) {
            Map<String, String> reqData = new LinkedHashMap<>();
            reqData.put("solicitante", req.solicitante());
            reqData.put("objetivo", req.objetivo());
            reqData.put("fechaHora", req.fechaHora().toString());
            solicitudes.add(reqData);
        }
        snapshot.put("solicitudesPendientes", solicitudes);

        // Información del ABB
        Map<String, Object> abbInfo = new LinkedHashMap<>();
        abbInfo.put("size", abb.size());
        abbInfo.put("altura", abb.altura());
        abbInfo.put("estaVacio", abb.estaVacio());
        snapshot.put("abb", abbInfo);

        // Información del grafo
        Map<String, Object> grafoInfo = new LinkedHashMap<>();
        grafoInfo.put("cantidadClientes", grafo.cantidadClientes());
        grafoInfo.put("cantidadConexiones", grafo.cantidadConexiones());
        grafoInfo.put("estaVacio", grafo.estaVacio());
        snapshot.put("grafo", grafoInfo);

        return snapshot;
    }

    // ---------------- HISTORIAL - CONSULTA PÚBLICA ----------------

    /**
     * Retorna una lista inmutable con todas las acciones del historial.
     * Orden: más reciente → más antiguo (mismo orden que undo()).
     * 
     * @return Lista inmutable de acciones (vacía si no hay historial)
     */
    public List<Action> getHistorialAcciones() {
        return List.copyOf(historial);
    }

    /**
     * Retorna una lista inmutable con las últimas N acciones del historial.
     * Orden: más reciente → más antiguo (mismo orden que undo()).
     * 
     * @param limit Número máximo de acciones a retornar (debe ser >= 0)
     * @return Lista inmutable de acciones limitadas
     * @throws IllegalArgumentException si limit es negativo
     */
    public List<Action> getHistorialAcciones(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit debe ser >= 0");
        }
        return historial.stream().limit(limit).toList();
    }
}
