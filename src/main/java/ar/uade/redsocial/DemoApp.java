package ar.uade.redsocial;

import ar.uade.redsocial.model.Action;
import ar.uade.redsocial.model.Cliente;
import ar.uade.redsocial.model.FollowRequest;
import ar.uade.redsocial.service.RedSocialEmpresarial;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Runner interactivo para pruebas manuales del TP "Red Social Empresarial"
 * Permite probar todas las features de Iteración 1/2/3 con un menú CLI
 * 
 * Ejecución:
 * mvn -q exec:java -Dexec.mainClass=ar.uade.redsocial.DemoApp
 */
public class DemoApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static RedSocialEmpresarial sistema = new RedSocialEmpresarial();
    private static String lastJsonPath = "demo.json";

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║   RED SOCIAL EMPRESARIAL - RUNNER DE TESTS MANUALES  ║");
        System.out.println("║        Iteración 1/2/3 - AyED II - UADE              ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");

        // Si se pasa argumento, cargar automáticamente
        if (args.length > 0) {
            lastJsonPath = args[0];
            cargarJSON(lastJsonPath);
        }

        menuPrincipal();
    }

    // ==================== MENÚ PRINCIPAL ====================

    private static void menuPrincipal() {
        while (true) {
            mostrarMenu();
            String opcion = leerLinea("Seleccione opción");

            try {
                switch (opcion) {
                    case "1" -> cargarJSON();
                    case "2" -> getSnapshot();
                    case "3" -> getClientes();
                    case "4" -> getScoringIndex();
                    case "5" -> getSiguiendo();
                    case "6" -> getConexionesVecinos();
                    case "7" -> getABBNivel4();
                    case "8" -> calcularDistancia();
                    case "9" -> crearCliente();
                    case "10" -> solicitarSeguir();
                    case "11" -> procesarSolicitud();
                    case "12" -> agregarConexion();
                    case "13" -> undo();
                    case "14" -> mostrarHistorial();
                    case "0" -> {
                        System.out.println("\n👋 Saliendo... Adiós!");
                        return;
                    }
                    default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
                }
            } catch (Exception e) {
                System.err.println("❌ ERROR: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("\n" + "─".repeat(60));
        }
    }

    private static void mostrarMenu() {
        System.out.println("\n╔════════════════════ MENÚ PRINCIPAL ════════════════════╗");
        System.out.println("║ [DATOS]                                                ║");
        System.out.println("║  1. 📂 Load demo JSON                                  ║");
        System.out.println("║  2. 📸 GET Snapshot (JSON pretty)                      ║");
        System.out.println("║                                                        ║");
        System.out.println("║ [CONSULTAS - GET]                                      ║");
        System.out.println("║  3. 👥 GET Clientes                                    ║");
        System.out.println("║  4. 📊 GET Scoring Index (TreeMap)                     ║");
        System.out.println("║  5. 👉 GET Siguiendo (de un cliente)                   ║");
        System.out.println("║  6. 🔗 GET Conexiones/Vecinos (grafo)                  ║");
        System.out.println("║  7. 🌲 ABB Nivel 4 (+ followersCount)                  ║");
        System.out.println("║  8. 📏 Calcular distancia (BFS)                        ║");
        System.out.println("║                                                        ║");
        System.out.println("║ [OPERACIONES - POST/PUT]                               ║");
        System.out.println("║  9. ➕ Crear cliente                                   ║");
        System.out.println("║ 10. 💌 Solicitar seguir (enqueue)                      ║");
        System.out.println("║ 11. ⚙️  Procesar solicitud (dequeue + confirmar)       ║");
        System.out.println("║ 12. 🔗 Agregar conexión (bidireccional)                ║");
        System.out.println("║ 13. ↩️  Undo (deshacer última acción)                  ║");
        System.out.println("║ 14. 📜 Historial de acciones                           ║");
        System.out.println("║                                                        ║");
        System.out.println("║  0. 🚪 Salir                                           ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
    }

    // ==================== OPERACIONES ====================

    private static void cargarJSON() {
        System.out.println("\n📂 CARGAR JSON");
        String ruta = leerLinea("Ruta del archivo JSON [" + lastJsonPath + "]");
        if (ruta.isBlank()) {
            ruta = lastJsonPath;
        }
        cargarJSON(ruta);
    }

    private static void cargarJSON(String ruta) {
        Path path = Path.of(ruta);
        if (!Files.exists(path)) {
            System.err.println("❌ Archivo no encontrado: " + path.toAbsolutePath());
            return;
        }

        try {
            sistema = new RedSocialEmpresarial(); // Reset
            sistema.loadFromJson(path.toString());
            lastJsonPath = ruta;
            System.out.println("✅ Datos cargados exitosamente desde: " + path.toAbsolutePath());
            System.out.println("   Clientes totales: " + sistema.cantidadClientes());
        } catch (Exception e) {
            System.err.println("❌ Error al cargar JSON: " + e.getMessage());
        }
    }

    private static void getSnapshot() {
        System.out.println("\n📸 SNAPSHOT DEL SISTEMA\n");
        Map<String, Object> snapshot = sistema.getSnapshot();
        System.out.println(gson.toJson(snapshot));
    }

    private static void getClientes() {
        System.out.println("\n👥 LISTA DE CLIENTES\n");
        List<Cliente> clientes = sistema.buscarPorScoringEntre(Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (clientes.isEmpty()) {
            System.out.println("❌ No hay clientes en el sistema.");
            return;
        }

        clientes.sort(Comparator.comparingInt(Cliente::getScoring).reversed());
        System.out.printf("Total: %d clientes\n\n", clientes.size());
        System.out.printf("%-20s | %8s | %10s | %15s\n", "Nombre", "Scoring", "Followers", "Siguiendo");
        System.out.println("─".repeat(70));
        for (Cliente c : clientes) {
            System.out.printf("%-20s | %8d | %10d | %15s\n",
                    c.getNombre(),
                    c.getScoring(),
                    c.getFollowersCount(),
                    c.getSiguiendo());
        }
    }

    private static void getScoringIndex() {
        System.out.println("\n📊 ÍNDICE POR SCORING (TreeMap)\n");
        Map<Integer, List<String>> index = new TreeMap<>();
        List<Cliente> todos = sistema.buscarPorScoringEntre(Integer.MIN_VALUE, Integer.MAX_VALUE);

        for (Cliente c : todos) {
            index.computeIfAbsent(c.getScoring(), k -> new ArrayList<>()).add(c.getNombre());
        }

        if (index.isEmpty()) {
            System.out.println("❌ No hay clientes en el índice.");
            return;
        }

        System.out.printf("%-10s | %s\n", "Scoring", "Clientes");
        System.out.println("─".repeat(60));
        for (Map.Entry<Integer, List<String>> entry : index.entrySet()) {
            System.out.printf("%-10d | %s\n", entry.getKey(), entry.getValue());
        }
    }

    private static void getSiguiendo() {
        System.out.println("\n👉 SIGUIENDO (de un cliente)\n");
        String nombre = leerLinea("Nombre del cliente");
        Cliente cliente = sistema.buscarPorNombre(nombre);

        if (cliente == null) {
            System.out.println("❌ Cliente no encontrado: " + nombre);
            return;
        }

        Set<String> siguiendo = cliente.getSiguiendo();
        System.out.printf("\n%s sigue a %d cliente(s):\n", nombre, siguiendo.size());
        if (siguiendo.isEmpty()) {
            System.out.println("  (no sigue a nadie)");
        } else {
            siguiendo.forEach(s -> System.out.println("  • " + s));
        }
    }

    private static void getConexionesVecinos() {
        System.out.println("\n🔗 CONEXIONES / VECINOS (grafo)\n");
        String nombre = leerLinea("Nombre del cliente");
        Cliente cliente = sistema.buscarPorNombre(nombre);

        if (cliente == null) {
            System.out.println("❌ Cliente no encontrado: " + nombre);
            return;
        }

        Set<String> vecinos = sistema.obtenerVecinos(nombre);
        System.out.printf("\n%s tiene %d vecino(s) en el grafo:\n", nombre, vecinos.size());
        if (vecinos.isEmpty()) {
            System.out.println("  (sin conexiones)");
        } else {
            vecinos.forEach(v -> System.out.println("  • " + v));
        }
    }

    private static void getABBNivel4() {
        System.out.println("\n🌲 ABB NIVEL 4 (ordenado por followersCount)\n");
        List<Cliente> nivel4 = sistema.obtenerClientesNivel4();

        if (nivel4.isEmpty()) {
            System.out.println("❌ No hay clientes en el nivel 4 del ABB.");
            System.out.println("   Posibles razones:");
            System.out.println("   - El árbol tiene menos de 5 niveles (0-4)");
            System.out.println("   - Los scorings insertados no generan suficiente profundidad");
            return;
        }

        System.out.printf("Total: %d cliente(s) en nivel 4\n\n", nivel4.size());
        System.out.printf("%-20s | %8s | %10s\n", "Nombre", "Scoring", "Followers");
        System.out.println("─".repeat(45));
        for (Cliente c : nivel4) {
            System.out.printf("%-20s | %8d | %10d\n",
                    c.getNombre(),
                    c.getScoring(),
                    c.getFollowersCount());
        }
    }

    private static void calcularDistancia() {
        System.out.println("\n📏 CALCULAR DISTANCIA (BFS)\n");
        String origen = leerLinea("Cliente origen");
        String destino = leerLinea("Cliente destino");

        if (sistema.buscarPorNombre(origen) == null) {
            System.out.println("❌ Cliente origen no encontrado: " + origen);
            return;
        }
        if (sistema.buscarPorNombre(destino) == null) {
            System.out.println("❌ Cliente destino no encontrado: " + destino);
            return;
        }

        try {
            int distancia = sistema.calcularDistancia(origen, destino);
            if (distancia == 0) {
                System.out.println("✅ Mismo cliente. Distancia: 0");
            } else if (distancia > 0) {
                System.out.printf("✅ Distancia entre '%s' y '%s': %d saltos\n", origen, destino, distancia);
            } else {
                System.out.println("❌ No hay camino entre los clientes.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al calcular distancia: " + e.getMessage());
        }
    }

    private static void crearCliente() {
        System.out.println("\n➕ CREAR CLIENTE\n");
        String nombre = leerLinea("Nombre");
        String scoringStr = leerLinea("Scoring");

        try {
            int scoring = Integer.parseInt(scoringStr);
            sistema.agregarCliente(nombre, scoring);
            System.out.println("✅ Cliente creado: " + nombre + " (scoring: " + scoring + ")");
        } catch (NumberFormatException e) {
            System.err.println("❌ Scoring inválido. Debe ser un número entero.");
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void solicitarSeguir() {
        System.out.println("\n💌 SOLICITAR SEGUIR (enqueue)\n");
        String solicitante = leerLinea("Solicitante");
        String objetivo = leerLinea("Objetivo");

        try {
            sistema.solicitarSeguir(solicitante, objetivo);
            System.out.println("✅ Solicitud encolada: " + solicitante + " -> " + objetivo);
            System.out.println("   Solicitudes pendientes: " + sistema.cantidadSolicitudesPendientes());
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void procesarSolicitud() {
        System.out.println("\n⚙️ PROCESAR SOLICITUD (dequeue + confirmar)\n");

        if (sistema.cantidadSolicitudesPendientes() == 0) {
            System.out.println("❌ No hay solicitudes pendientes para procesar.");
            return;
        }

        FollowRequest solicitud = sistema.procesarSiguienteSolicitud();
        if (solicitud == null) {
            System.out.println("❌ Cola vacía.");
            return;
        }

        System.out.printf("📤 Procesada: %s -> %s\n", solicitud.solicitante(), solicitud.objetivo());

        try {
            sistema.confirmarSeguimiento(solicitud.solicitante(), solicitud.objetivo());
            System.out.println("✅ Seguimiento confirmado.");
            System.out.println("   Solicitudes restantes: " + sistema.cantidadSolicitudesPendientes());
        } catch (Exception e) {
            System.err.println("⚠️ Solicitud procesada pero falló confirmación: " + e.getMessage());
        }
    }

    private static void agregarConexion() {
        System.out.println("\n🔗 AGREGAR CONEXIÓN (bidireccional)\n");
        String cliente1 = leerLinea("Cliente 1");
        String cliente2 = leerLinea("Cliente 2");

        try {
            sistema.agregarConexion(cliente1, cliente2);
            System.out.println("✅ Conexión agregada: " + cliente1 + " ↔ " + cliente2);
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void undo() {
        System.out.println("\n↩️ UNDO (deshacer última acción)\n");

        Optional<Action> accion = sistema.undo();
        if (accion.isEmpty()) {
            System.out.println("ℹ️ No hay acciones para deshacer.");
            return;
        }

        Action a = accion.get();
        System.out.println("✅ Acción deshecha:");
        System.out.println("   Tipo: " + a.type());
        System.out.println("   Detalle: " + a.detalle());
        System.out.println("   Fecha/Hora: " + a.fechaHora());
    }

    private static void mostrarHistorial() {
        System.out.println("\n📜 HISTORIAL DE ACCIONES\n");

        String limitStr = leerLinea("¿Cuántas acciones mostrar? [default: 20]");
        int limit = 20;

        if (!limitStr.isEmpty()) {
            try {
                limit = Integer.parseInt(limitStr);
                if (limit < 0) {
                    System.err.println("⚠️ Límite inválido, usando 20 por defecto.");
                    limit = 20;
                }
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Límite inválido, usando 20 por defecto.");
            }
        }

        List<Action> historial = sistema.getHistorialAcciones(limit);

        if (historial.isEmpty()) {
            System.out.println("ℹ️ Historial vacío (0 acciones).");
            return;
        }

        System.out.printf("Mostrando las últimas %d acciones (de %d total):\n\n",
                historial.size(), sistema.getHistorialAcciones().size());

        System.out.printf("%-4s | %-19s | %-20s | %s\n", "#", "Fecha/Hora", "Tipo", "Detalle");
        System.out.println("─".repeat(100));

        for (int i = 0; i < historial.size(); i++) {
            Action a = historial.get(i);
            String timestamp = a.fechaHora().toString().replace('T', ' ');
            String detalle = a.detalle();

            // Truncar detalle si es muy largo
            if (detalle.length() > 50) {
                detalle = detalle.substring(0, 47) + "...";
            }

            System.out.printf("%-4d | %-19s | %-20s | %s\n",
                    (i + 1), timestamp, a.type(), detalle);
        }
    }

    // ==================== UTILIDADES ====================

    private static String leerLinea(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }
}
