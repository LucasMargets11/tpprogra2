package ar.uade.redsocial.service;

import ar.uade.redsocial.dto.ClienteDTO;
import ar.uade.redsocial.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    // Clase auxiliar para mapear la raíz del JSON
    private static class JsonDataWrapper {
        List<ClienteDTO> clientes;
    }

    // ---------------- CARGA DE DATOS ----------------

    public void loadFromJson(String ruta) {
        Path path = Paths.get(ruta);
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
                
                // addClienteInterno ya valida scoring < 0
                Cliente nuevo = addClienteInterno(dto.nombre, dto.scoring);
                
                // Parseo tolerante de listas
                if (dto.siguiendo != null) {
                    for (String seguido : dto.siguiendo) {
                        nuevo.seguirA(seguido);
                    }
                }
                if (dto.conexiones != null) {
                    for (String conexion : dto.conexiones) {
                        nuevo.agregarConexion(conexion);
                    }
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
                LocalDateTime.now()
        ));
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
        NavigableMap<Integer, Set<String>> subMapa =
                indicePorScoring.subMap(min, true, max, true);

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
        if (eliminado == null) return; // Ya no existía (raro pero defensivo)

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

        // Recuperar y remover la última solicitud (LIFO behavior sobre la estructura usada como Queue)
        // La solicitud correspondiente a ESTA acción debería ser la última agregada.
        FollowRequest lastRequest = colaSeguimientos.removeLast();

        // Validar integridad
        Object payload = action.payload();
        if (payload instanceof FollowRequest originalRequest) {
            if (!originalRequest.equals(lastRequest)) {
                // Rollback parcial (volvemos a poner lo que sacamos)
                colaSeguimientos.addLast(lastRequest);
                throw new IllegalStateException(
                    String.format("Corrupción de historial: Se intentó deshacer '%s' pero en la cola estaba '%s'", originalRequest, lastRequest)
                );
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
                LocalDateTime.now()
        ));
    }

    public FollowRequest procesarSiguienteSolicitud() {
        return colaSeguimientos.pollFirst(); // FIFO
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
}
