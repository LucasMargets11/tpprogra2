package ar.uade.redsocial;

import ar.uade.redsocial.model.Action;
import ar.uade.redsocial.model.Cliente;
import ar.uade.redsocial.model.FollowRequest;
import ar.uade.redsocial.service.JsonLoader;
import ar.uade.redsocial.service.RedSocialEmpresarial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DemoApp {

    public static void main(String[] args) {
        String jsonArg = args.length > 0 ? args[0] : "demo.json";
        Path jsonPath = Path.of(jsonArg).toAbsolutePath().normalize();
        RedSocialEmpresarial sistema = new RedSocialEmpresarial();

        if (!cargarDatos(jsonPath, sistema)) {
            System.err.println("No se pudieron cargar datos. Revise la ruta y el formato del JSON.");
            System.exit(1);
            return;
        }

        System.out.println("=== Demo Red Social Empresarial ===");
        System.out.println("Archivo cargado: " + jsonPath);
        System.out.println("Clientes totales: " + sistema.cantidadClientes());

        mostrarClientesPorScoring(sistema, 0, 100);
        mostrarBusquedaPorNombre(sistema);
        demostrarFIFO(sistema);
        demostrarUndo(sistema);
    }

    private static boolean cargarDatos(Path path, RedSocialEmpresarial sistema) {
        if (Files.exists(path)) {
            try {
                sistema.loadFromJson(path.toString());
                System.out.println("Datos cargados con RedSocialEmpresarial.loadFromJson");
                return true;
            } catch (RuntimeException ex) {
                System.err.println("Error usando loadFromJson: " + ex.getMessage());
            }
        }

        try {
            new JsonLoader().cargarDesdeArchivo(path.toString(), sistema);
            System.out.println("Datos cargados mediante JsonLoader");
            return true;
        } catch (IOException ex) {
            System.err.println("Error usando JsonLoader: " + ex.getMessage());
        }

        return false;
    }

    private static void mostrarClientesPorScoring(RedSocialEmpresarial sistema, int min, int max) {
        List<Cliente> clientes = sistema.buscarPorScoringEntre(min, max);
        clientes.sort(Comparator.comparingInt(Cliente::getScoring).reversed());
        System.out.println("\nClientes con scoring entre " + min + " y " + max + ":");
        if (clientes.isEmpty()) {
            System.out.println(" - (sin resultados)");
            return;
        }
        for (Cliente cliente : clientes) {
            System.out.printf(" - %s (scoring %d)\n", cliente.getNombre(), cliente.getScoring());
        }
    }

    private static void mostrarBusquedaPorNombre(RedSocialEmpresarial sistema) {
        List<Cliente> todos = sistema.buscarPorScoringEntre(Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (todos.isEmpty()) {
            System.out.println("\nNo hay clientes para mostrar búsqueda por nombre.");
            return;
        }

        Cliente encontrado = sistema.buscarPorNombre("Hugo");
        if (encontrado == null) {
            encontrado = todos.get(0);
        }

        System.out.println("\nResultado de buscarPorNombre('" + encontrado.getNombre() + "'):");
        System.out.println(" - Nombre: " + encontrado.getNombre());
        System.out.println(" - Scoring: " + encontrado.getScoring());
        System.out.println(" - Siguiendo: " + encontrado.getSiguiendo());
        System.out.println(" - Conexiones: " + encontrado.getConexiones());
    }

    private static void demostrarFIFO(RedSocialEmpresarial sistema) {
        List<Cliente> clientes = sistema.buscarPorScoringEntre(Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (clientes.size() < 2) {
            System.out.println("\nSe requieren al menos dos clientes para demostrar la cola de solicitudes.");
            return;
        }

        String primero = clientes.get(0).getNombre();
        String segundo = clientes.get(1).getNombre();
        sistema.solicitarSeguir(primero, segundo);
        sistema.solicitarSeguir(segundo, primero);
        System.out.println("\nSolicitudes creadas: " + sistema.cantidadSolicitudesPendientes());
        System.out.println("Se procesarán " + sistema.cantidadSolicitudesPendientes() + " solicitudes en orden FIFO");

        FollowRequest salida1 = sistema.procesarSiguienteSolicitud();
        FollowRequest salida2 = sistema.procesarSiguienteSolicitud();
        System.out.println("Procesando cola FIFO:");
        imprimirSolicitud(salida1);
        imprimirSolicitud(salida2);
    }

    private static void demostrarUndo(RedSocialEmpresarial sistema) {
        String demoName = generarNombreTemporal(sistema);
        int demoScore = 42;
        sistema.agregarCliente(demoName, demoScore);
        System.out.println("\nClientes tras alta temporal: " + sistema.cantidadClientes());
        System.out.println("Cliente agregado para demo: " + demoName);

        Optional<Action> undo = sistema.undo();
        if (undo.isPresent()) {
            System.out.println("Acción deshecha: " + undo.get().type() + " -> " + undo.get().detalle());
        } else {
            System.out.println("No había acciones para deshacer.");
        }
        System.out.println("Clientes luego del undo: " + sistema.cantidadClientes());
        System.out.println("¿Cliente demo sigue presente?: " + (sistema.buscarPorNombre(demoName) != null));
    }

    private static void imprimirSolicitud(FollowRequest request) {
        if (request == null) {
            System.out.println(" - (sin solicitudes pendientes)");
            return;
        }
        System.out.printf(" - %s -> %s (%s)\n", request.solicitante(), request.objetivo(), request.fechaHora());
    }

    private static String generarNombreTemporal(RedSocialEmpresarial sistema) {
        String base = "Demo" + UUID.randomUUID().toString().substring(0, 8);
        while (sistema.buscarPorNombre(base) != null) {
            base = "Demo" + UUID.randomUUID().toString().substring(0, 8);
        }
        return base;
    }
}
