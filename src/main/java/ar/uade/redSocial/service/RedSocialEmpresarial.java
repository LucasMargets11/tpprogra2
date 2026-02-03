package ar.uade.redsocial.service;

import ar.uade.redsocial.model.*;

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

    // nombre -> Cliente
    private final Map<String, Cliente> clientesPorNombre = new HashMap<>();

    // scoring -> conjunto de nombres (ordenado)
    private final NavigableMap<Integer, Set<String>> indicePorScoring = new TreeMap<>();

    // Historial de acciones (PILA)
    private final Deque<Action> historial = new ArrayDeque<>();

    // Solicitudes de seguimiento (COLA)
    private final Deque<FollowRequest> colaSeguimientos = new ArrayDeque<>();

    // ---------------- CLIENTES ----------------

    public void agregarCliente(String nombre, int scoring) {
        validarNombre(nombre);
        validarScoring(scoring);

        if (clientesPorNombre.containsKey(nombre)) {
            throw new IllegalArgumentException("Ya existe el cliente: " + nombre);
        }

        Cliente cliente = new Cliente(nombre, scoring);
        clientesPorNombre.put(nombre, cliente);

        indicePorScoring
                .computeIfAbsent(scoring, k -> new HashSet<>())
                .add(nombre);

        registrarAccion(new Action(
                ActionType.AGREGAR_CLIENTE,
                nombre,
                LocalDateTime.now()
        ));
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

    // ---------------- HISTORIAL (PILA) ----------------

    private void registrarAccion(Action accion) {
        historial.push(accion); // O(1)
    }

    public Action deshacerUltimaAccion() {
        if (historial.isEmpty()) return null;

        Action ultima = historial.pop();

        if (ultima.type() == ActionType.AGREGAR_CLIENTE) {
            eliminarClienteInterno(ultima.detalle());
        }

        return ultima;
    }

    private void eliminarClienteInterno(String nombre) {
        Cliente eliminado = clientesPorNombre.remove(nombre);
        if (eliminado == null) return;

        Set<String> nombres = indicePorScoring.get(eliminado.getScoring());
        if (nombres != null) {
            nombres.remove(nombre);
            if (nombres.isEmpty()) {
                indicePorScoring.remove(eliminado.getScoring());
            }
        }
    }

    // ---------------- SEGUIMIENTOS (COLA) ----------------

    public void solicitarSeguir(String solicitante, String objetivo) {
        validarNombre(solicitante);
        validarNombre(objetivo);

        if (!clientesPorNombre.containsKey(solicitante) ||
            !clientesPorNombre.containsKey(objetivo)) {
            throw new IllegalArgumentException("Cliente inexistente");
        }

        colaSeguimientos.addLast(
                new FollowRequest(solicitante, objetivo, LocalDateTime.now())
        );

        registrarAccion(new Action(
                ActionType.SOLICITAR_SEGUIR,
                solicitante + " -> " + objetivo,
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
