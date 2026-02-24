package ar.uade.redsocial;

import ar.uade.redsocial.dto.ClienteDTO;
import ar.uade.redsocial.model.Action;
import ar.uade.redsocial.model.Cliente;
import ar.uade.redsocial.model.FollowRequest;
import ar.uade.redsocial.service.RedSocialEmpresarial;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RedSocialEmpresarialTest {

    private RedSocialEmpresarial red;

    @BeforeEach
    void setUp() {
        red = new RedSocialEmpresarial();
    }

    // ---------------- TEST JSON (ESTRUCTURA OBJETO) ----------------

    @Test
    void testLoadFromJson_ok_conFormatoDelTP() throws Exception {
        // Formato esperado: { "clientes": [ ... ] }
        String jsonContent = """
                {
                  "clientes": [
                    { "nombre": "Alice", "scoring": 95, "siguiendo": ["Bob"], "conexiones": [] },
                    { "nombre": "Bob", "scoring": 80 }
                  ]
                }
                """;

        Path tempFile = Files.createTempFile("data_ok", ".json");
        Files.writeString(tempFile, jsonContent);

        red.loadFromJson(tempFile.toString());

        assertEquals(2, red.cantidadClientes());

        Cliente alice = red.buscarPorNombre("Alice");
        assertNotNull(alice);
        assertEquals(95, alice.getScoring());
        assertTrue(alice.getSiguiendo().contains("Bob"));

        Cliente bob = red.buscarPorNombre("Bob");
        assertNotNull(bob);
        assertEquals(80, bob.getScoring());

        // Verificar índice
        assertEquals(1, red.buscarPorScoring(95).size());

        // Verificar historial vacío tras carga
        assertTrue(red.undo().isEmpty(), "La carga JSON no debe registrar historial");

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testLoadFromJson_jsonInvalido_falla() throws Exception {
        String jsonContent = "{ \"clientes\": [ .... error ... ] }";
        Path tempFile = Files.createTempFile("data_err", ".json");
        Files.writeString(tempFile, jsonContent);

        assertThrows(IllegalArgumentException.class, () -> red.loadFromJson(tempFile.toString()));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testLoadFromJson_duplicados_falla() throws Exception {
        String jsonContent = """
                {
                  "clientes": [
                    { "nombre": "Clon", "scoring": 10 },
                    { "nombre": "Clon", "scoring": 20 }
                  ]
                }
                """;
        Path tempFile = Files.createTempFile("data_dup", ".json");
        Files.writeString(tempFile, jsonContent);

        assertThrows(IllegalArgumentException.class, () -> red.loadFromJson(tempFile.toString()));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testLoadFromJson_scoringNegativo_falla() throws Exception {
        String jsonContent = """
                { "clientes": [ { "nombre": "Malo", "scoring": -5 } ] }
                """;
        Path tempFile = Files.createTempFile("data_neg", ".json");
        Files.writeString(tempFile, jsonContent);

        assertThrows(IllegalArgumentException.class, () -> red.loadFromJson(tempFile.toString()));
        Files.deleteIfExists(tempFile);
    }

    // ---------------- TEST FIFO & UNDO REQUEST ----------------

    @Test
    void testFIFO_solicitarYProcesar() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 10);

        red.solicitarSeguir("A", "B");
        red.solicitarSeguir("B", "A");

        // Primera solicitud sale primero
        FollowRequest req1 = red.procesarSiguienteSolicitud();
        assertEquals("A", req1.solicitante());
        assertEquals("B", req1.objetivo());

        // Segunda solicitud sale segunda
        FollowRequest req2 = red.procesarSiguienteSolicitud();
        assertEquals("B", req2.solicitante());
        assertEquals("A", req2.objetivo());
    }

    @Test
    void testUndo_RequestFollow() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 10);

        red.solicitarSeguir("A", "B"); // Req 1
        red.solicitarSeguir("B", "A"); // Req 2

        assertEquals(2, red.cantidadSolicitudesPendientes());

        // Undo de la última acción (Req 2)
        Optional<Action> undone = red.undo();
        assertTrue(undone.isPresent());
        assertTrue(undone.get().detalle().contains("B -> A"));

        assertEquals(1, red.cantidadSolicitudesPendientes());

        // Al procesar, debe estar Req 1 intacta
        FollowRequest pending = red.procesarSiguienteSolicitud();
        assertEquals("A", pending.solicitante());
        assertEquals("B", pending.objetivo());
    }

    // ---------------- TEST UNDO ADD_CLIENT (Cleanup) ----------------

    @Test
    void testUndo_AddClient_CleanReferences() {
        red.agregarCliente("A", 100);
        red.agregarCliente("B", 200);

        // Simulamos que B sigue a A (via backdoor o si existiera API pública en iter 2)
        // Como 'Cliente' tiene setSeguiendo público en el contexto actual:
        red.buscarPorNombre("B").seguirA("A");

        // Verificamos estado previo al undo
        assertTrue(red.buscarPorNombre("B").getSiguiendo().contains("A"));

        // A fue el primero, B el segundo. Stack: [Add A, Add B].
        // Undo Add B:
        red.undo(); // borra B
        assertNull(red.buscarPorNombre("B"));
        assertNotNull(red.buscarPorNombre("A")); // A sigue existiendo

        // Undo Add A:
        // Pero primero vamos a re-agregar B para probar el caso inverso (Borrar A
        // cuando B lo sigue)
        // Reset escenario
        red = new RedSocialEmpresarial();
        red.agregarCliente("B", 200);
        red.agregarCliente("A", 100); // A es el último en stack

        // B sigue a A
        red.buscarPorNombre("B").seguirA("A");

        // Undo Add A
        red.undo();

        assertNull(red.buscarPorNombre("A"));
        // VALIDACIÓN CLAVE: B ya no debe seguir a A porque A no existe
        assertFalse(red.buscarPorNombre("B").getSiguiendo().contains("A"),
                "Undo debe eliminar referencias al cliente borrado");

        // Validar indices
        assertEquals(0, red.buscarPorScoring(100).size()); // A borrado
        assertEquals(1, red.buscarPorScoring(200).size()); // B existe
    }

    // ---------------- TEST BUSQUEDAS ----------------

    @Test
    void testBuscadores() {
        red.agregarCliente("Min", 10);
        red.agregarCliente("Max", 50);

        assertEquals(1, red.buscarPorScoring(10).size());

        List<Cliente> rango = red.buscarPorScoringEntre(10, 50); // Inclusivo
        assertEquals(2, rango.size());
    }

    // ---------------- TESTS ADICIONALES ITERACIÓN 1 ----------------

    @Test
    void testLoadFromJson_jsonVacio_oSinClientes() throws Exception {
        // JSON vacío (solo wrapper sin clientes)
        String jsonContent = """
                { "clientes": [] }
                """;
        Path tempFile = Files.createTempFile("data_empty", ".json");
        Files.writeString(tempFile, jsonContent);

        red.loadFromJson(tempFile.toString());

        assertEquals(0, red.cantidadClientes());
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testAgregarCliente_nombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> red.agregarCliente("", 10));
        assertThrows(IllegalArgumentException.class, () -> red.agregarCliente("   ", 10));
        assertThrows(IllegalArgumentException.class, () -> red.agregarCliente(null, 10));
    }

    @Test
    void testBuscarPorNombre_existente() {
        red.agregarCliente("Existente", 50);
        Cliente encontrado = red.buscarPorNombre("Existente");
        assertNotNull(encontrado);
        assertEquals("Existente", encontrado.getNombre());
        assertEquals(50, encontrado.getScoring());
    }

    @Test
    void testBuscarPorNombre_inexistente() {
        Cliente noEncontrado = red.buscarPorNombre("NoExiste");
        assertNull(noEncontrado);
    }

    @Test
    void testBuscarPorScoring_exacto() {
        red.agregarCliente("A", 100);
        red.agregarCliente("B", 100);
        red.agregarCliente("C", 50);

        List<Cliente> con100 = red.buscarPorScoring(100);
        assertEquals(2, con100.size());

        List<Cliente> con50 = red.buscarPorScoring(50);
        assertEquals(1, con50.size());

        List<Cliente> con999 = red.buscarPorScoring(999);
        assertEquals(0, con999.size());
    }

    @Test
    void testBuscarPorScoring_rango() {
        red.agregarCliente("Min", 10);
        red.agregarCliente("Med", 50);
        red.agregarCliente("Max", 90);

        List<Cliente> rango = red.buscarPorScoringEntre(20, 80);
        assertEquals(1, rango.size());
        assertEquals("Med", rango.get(0).getNombre());

        List<Cliente> todos = red.buscarPorScoringEntre(10, 90);
        assertEquals(3, todos.size());

        List<Cliente> ninguno = red.buscarPorScoringEntre(100, 200);
        assertEquals(0, ninguno.size());
    }

    @Test
    void testUndo_sinAcciones() {
        Optional<Action> resultado = red.undo();
        assertTrue(resultado.isEmpty(), "Undo sin acciones debe retornar Optional vacío");
    }

    @Test
    void testProcesarSolicitud_colaVacia() {
        FollowRequest resultado = red.procesarSiguienteSolicitud();
        assertNull(resultado, "Procesar solicitud con cola vacía debe retornar null");
    }

    @Test
    void testSolicitudesFIFO_ordenCorrecto() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);
        red.agregarCliente("C", 30);

        red.solicitarSeguir("A", "B");
        red.solicitarSeguir("B", "C");
        red.solicitarSeguir("C", "A");

        assertEquals(3, red.cantidadSolicitudesPendientes());

        // Primera en salir
        FollowRequest req1 = red.procesarSiguienteSolicitud();
        assertEquals("A", req1.solicitante());
        assertEquals("B", req1.objetivo());

        // Segunda en salir
        FollowRequest req2 = red.procesarSiguienteSolicitud();
        assertEquals("B", req2.solicitante());
        assertEquals("C", req2.objetivo());

        // Tercera en salir
        FollowRequest req3 = red.procesarSiguienteSolicitud();
        assertEquals("C", req3.solicitante());
        assertEquals("A", req3.objetivo());

        assertEquals(0, red.cantidadSolicitudesPendientes());
    }

    @Test
    void testLoadFromJson_clienteDuplicado() throws Exception {
        // Ya existe en tests anteriores, pero lo nombramos explícitamente
        String jsonContent = """
                {
                  "clientes": [
                    { "nombre": "Duplicado", "scoring": 10 },
                    { "nombre": "Duplicado", "scoring": 20 }
                  ]
                }
                """;
        Path tempFile = Files.createTempFile("data_dup2", ".json");
        Files.writeString(tempFile, jsonContent);

        assertThrows(IllegalArgumentException.class, () -> red.loadFromJson(tempFile.toString()));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testRegistrarAccion_yUndo() {
        red.agregarCliente("Test", 50);
        assertEquals(1, red.cantidadClientes());

        Optional<Action> undone = red.undo();
        assertTrue(undone.isPresent());
        assertEquals("Test", undone.get().detalle());

        assertEquals(0, red.cantidadClientes());
        assertNull(red.buscarPorNombre("Test"));
    }

    // ============================================================
    // ITERACIÓN 2: SEGUIMIENTOS (MAX 2) + ABB + NIVEL 4
    // ============================================================

    @Test
    void testMaximoDosSeguimientos() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);
        red.agregarCliente("C", 30);
        red.agregarCliente("D", 40);

        Cliente clienteA = red.buscarPorNombre("A");

        // Primer seguimiento: OK
        clienteA.seguirA("B");
        assertEquals(1, clienteA.getSiguiendo().size());

        // Segundo seguimiento: OK
        clienteA.seguirA("C");
        assertEquals(2, clienteA.getSiguiendo().size());

        // Tercer seguimiento: FALLA
        assertThrows(IllegalStateException.class, () -> clienteA.seguirA("D"));
        assertEquals(2, clienteA.getSiguiendo().size()); // No cambió
    }

    @Test
    void testNoSeguirseASiMismo() {
        red.agregarCliente("Narciso", 50);
        Cliente narciso = red.buscarPorNombre("Narciso");

        assertThrows(IllegalArgumentException.class, () -> narciso.seguirA("Narciso"));
        assertEquals(0, narciso.getSiguiendo().size());
    }

    @Test
    void testNoDuplicarSeguimiento() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);

        Cliente clienteA = red.buscarPorNombre("A");

        clienteA.seguirA("B");
        assertEquals(1, clienteA.getSiguiendo().size());

        // Intentar duplicar
        assertThrows(IllegalArgumentException.class, () -> clienteA.seguirA("B"));
        assertEquals(1, clienteA.getSiguiendo().size()); // No cambió
    }

    @Test
    void testConfirmarSeguimiento_incrementaFollowers() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);

        Cliente clienteB = red.buscarPorNombre("B");
        assertEquals(0, clienteB.getFollowersCount());

        red.confirmarSeguimiento("A", "B");

        assertEquals(1, clienteB.getFollowersCount());
        assertTrue(red.buscarPorNombre("A").getSiguiendo().contains("B"));
    }

    @Test
    void testLoadFromJson_siguiendoRespetaMax2() throws Exception {
        // JSON inválido: cliente con 3 seguimientos
        String jsonInvalido = """
                {
                  "clientes": [
                    { "nombre": "Infractor", "scoring": 50, "siguiendo": ["A", "B", "C"] }
                  ]
                }
                """;
        Path tempFile = Files.createTempFile("data_max_seguimientos", ".json");
        Files.writeString(tempFile, jsonInvalido);

        assertThrows(IllegalArgumentException.class, () -> red.loadFromJson(tempFile.toString()));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testLoadFromJson_actualizaFollowers() throws Exception {
        String jsonContent = """
                {
                  "clientes": [
                    { "nombre": "Popular", "scoring": 100, "siguiendo": [] },
                    { "nombre": "Fan1", "scoring": 50, "siguiendo": ["Popular"] },
                    { "nombre": "Fan2", "scoring": 40, "siguiendo": ["Popular"] }
                  ]
                }
                """;
        Path tempFile = Files.createTempFile("data_followers", ".json");
        Files.writeString(tempFile, jsonContent);

        red.loadFromJson(tempFile.toString());

        Cliente popular = red.buscarPorNombre("Popular");
        assertEquals(2, popular.getFollowersCount());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testABB_insertar_yOrden() {
        red.agregarCliente("C", 50);
        red.agregarCliente("A", 10);
        red.agregarCliente("E", 90);
        red.agregarCliente("B", 20);
        red.agregarCliente("D", 70);

        // Verificar que ABB no está vacío
        assertFalse(red.getABB().estaVacio());
        assertEquals(5, red.getABB().size());

        // Verificar orden inorder (debe estar ordenado por scoring)
        List<Cliente> inorder = red.getABB().inorder();
        assertEquals(5, inorder.size());
        assertEquals("A", inorder.get(0).getNombre());
        assertEquals("B", inorder.get(1).getNombre());
        assertEquals("C", inorder.get(2).getNombre());
        assertEquals("D", inorder.get(3).getNombre());
        assertEquals("E", inorder.get(4).getNombre());
    }

    @Test
    void testABB_obtenerNivel4() {
        // Construir árbol con al menos 4 niveles
        // Nivel 0: 50
        // Nivel 1: 25, 75
        // Nivel 2: 10, 40, 60, 90
        // Nivel 3: 5, 15, 30, 45, 55, 65, 85, 95
        // Nivel 4: 3, 7, 12, 18, 28, 33, 43, 48, ... (muchos más posibles)

        red.agregarCliente("N50", 50);
        red.agregarCliente("N25", 25);
        red.agregarCliente("N75", 75);
        red.agregarCliente("N10", 10);
        red.agregarCliente("N40", 40);
        red.agregarCliente("N60", 60);
        red.agregarCliente("N90", 90);
        red.agregarCliente("N5", 5);
        red.agregarCliente("N15", 15);
        red.agregarCliente("N30", 30);
        red.agregarCliente("N45", 45);
        red.agregarCliente("N55", 55);
        red.agregarCliente("N65", 65);
        red.agregarCliente("N85", 85);
        red.agregarCliente("N95", 95);
        // Nivel 4:
        red.agregarCliente("N3", 3);
        red.agregarCliente("N7", 7);

        List<Cliente> nivel4 = red.obtenerClientesNivel4();

        // El nivel 4 debería tener al menos 2 clientes (N3, N7)
        assertTrue(nivel4.size() >= 2);
        assertTrue(nivel4.stream().anyMatch(c -> c.getNombre().equals("N3")));
        assertTrue(nivel4.stream().anyMatch(c -> c.getNombre().equals("N7")));
    }

    @Test
    void testABB_nivel4_ordenadoPorFollowers() throws Exception {
        // Cargar JSON que tenga clientes en nivel 4 con diferentes followersCount
        String jsonContent = """
                {
                  "clientes": [
                    { "nombre": "N50", "scoring": 50, "siguiendo": [] },
                    { "nombre": "N25", "scoring": 25, "siguiendo": [] },
                    { "nombre": "N75", "scoring": 75, "siguiendo": [] },
                    { "nombre": "N10", "scoring": 10, "siguiendo": [] },
                    { "nombre": "N40", "scoring": 40, "siguiendo": [] },
                    { "nombre": "N60", "scoring": 60, "siguiendo": [] },
                    { "nombre": "N90", "scoring": 90, "siguiendo": [] },
                    { "nombre": "N5", "scoring": 5, "siguiendo": [] },
                    { "nombre": "N15", "scoring": 15, "siguiendo": [] },
                    { "nombre": "N30", "scoring": 30, "siguiendo": [] },
                    { "nombre": "N45", "scoring": 45, "siguiendo": [] },
                    { "nombre": "N55", "scoring": 55, "siguiendo": [] },
                    { "nombre": "N65", "scoring": 65, "siguiendo": [] },
                    { "nombre": "N85", "scoring": 85, "siguiendo": [] },
                    { "nombre": "N95", "scoring": 95, "siguiendo": [] },
                    { "nombre": "N3", "scoring": 3, "siguiendo": [] },
                    { "nombre": "N7", "scoring": 7, "siguiendo": [] },
                    { "nombre": "Fan1", "scoring": 1, "siguiendo": ["N3", "N7"] },
                    { "nombre": "Fan2", "scoring": 2, "siguiendo": ["N3"] }
                  ]
                }
                """;
        Path tempFile = Files.createTempFile("data_nivel4", ".json");
        Files.writeString(tempFile, jsonContent);

        red.loadFromJson(tempFile.toString());

        List<Cliente> nivel4 = red.obtenerClientesNivel4();

        // N3 debería tener 2 followers, N7 debería tener 1
        Cliente n3 = red.buscarPorNombre("N3");
        Cliente n7 = red.buscarPorNombre("N7");
        assertEquals(2, n3.getFollowersCount());
        assertEquals(1, n7.getFollowersCount());

        // El nivel 4 debe estar ordenado por followersCount descendente
        if (nivel4.size() >= 2) {
            // Primer elemento debe tener >= followersCount que el segundo
            assertTrue(nivel4.get(0).getFollowersCount() >= nivel4.get(1).getFollowersCount());
        }

        Files.deleteIfExists(tempFile);
    }

    // ============================================================
    // ITERACIÓN 3: GRAFO + VECINOS + BFS DISTANCIA
    // ============================================================

    @Test
    void testVecinos_basico() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);
        red.agregarCliente("C", 30);

        red.agregarConexion("A", "B");
        red.agregarConexion("A", "C");

        var vecinosA = red.obtenerVecinos("A");
        assertEquals(2, vecinosA.size());
        assertTrue(vecinosA.contains("B"));
        assertTrue(vecinosA.contains("C"));

        var vecinosB = red.obtenerVecinos("B");
        assertEquals(1, vecinosB.size());
        assertTrue(vecinosB.contains("A"));
    }

    @Test
    void testAgregarConexion_bidireccional() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);

        red.agregarConexion("A", "B");

        assertTrue(red.existeConexion("A", "B"));
        assertTrue(red.existeConexion("B", "A")); // Bidireccional

        assertTrue(red.obtenerVecinos("A").contains("B"));
        assertTrue(red.obtenerVecinos("B").contains("A"));
    }

    @Test
    void testDistancia_0_mismoCliente() {
        red.agregarCliente("A", 10);

        assertEquals(0, red.calcularDistancia("A", "A"));
    }

    @Test
    void testDistancia_1_vecinos() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);

        red.agregarConexion("A", "B");

        assertEquals(1, red.calcularDistancia("A", "B"));
        assertEquals(1, red.calcularDistancia("B", "A")); // Simétrico
    }

    @Test
    void testDistancia_2_oMas() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);
        red.agregarCliente("C", 30);
        red.agregarCliente("D", 40);

        red.agregarConexion("A", "B");
        red.agregarConexion("B", "C");
        red.agregarConexion("C", "D");

        assertEquals(2, red.calcularDistancia("A", "C"));
        assertEquals(3, red.calcularDistancia("A", "D"));
        assertEquals(1, red.calcularDistancia("B", "C"));
    }

    @Test
    void testDistancia_sinCamino_devuelveMenos1() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);
        red.agregarCliente("C", 30);
        red.agregarCliente("D", 40);

        red.agregarConexion("A", "B");
        red.agregarConexion("C", "D");

        // A y B están conectados, C y D están conectados, pero no hay camino entre
        // ellos
        assertEquals(-1, red.calcularDistancia("A", "C"));
        assertEquals(-1, red.calcularDistancia("B", "D"));
        assertEquals(-1, red.calcularDistancia("A", "D"));
    }

    @Test
    void testDistancia_grafoConCiclo() {
        red.agregarCliente("A", 10);
        red.agregarCliente("B", 20);
        red.agregarCliente("C", 30);
        red.agregarCliente("D", 40);

        // Crear un ciclo: A-B-C-D-A
        red.agregarConexion("A", "B");
        red.agregarConexion("B", "C");
        red.agregarConexion("C", "D");
        red.agregarConexion("D", "A");

        // Distancia más corta entre A y C: A-B-C (2 saltos), no A-D-C (2 saltos
        // también)
        assertEquals(2, red.calcularDistancia("A", "C"));

        // Distancia más corta entre B y D: B-C-D (2 saltos)
        assertEquals(2, red.calcularDistancia("B", "D"));

        // Con ciclo, cualquier par tiene camino
        assertEquals(1, red.calcularDistancia("A", "D"));
    }

    @Test
    void testLoadFromJson_conexionesCargaEnGrafo() throws Exception {
        String jsonContent = """
                {
                  "clientes": [
                    { "nombre": "Ana", "scoring": 95, "siguiendo": [], "conexiones": ["Bruno", "Carla"] },
                    { "nombre": "Bruno", "scoring": 82, "siguiendo": [], "conexiones": ["Ana"] },
                    { "nombre": "Carla", "scoring": 67, "siguiendo": [], "conexiones": ["Ana", "Diego"] },
                    { "nombre": "Diego", "scoring": 40, "siguiendo": [], "conexiones": ["Carla"] }
                  ]
                }
                """;
        Path tempFile = Files.createTempFile("data_grafo", ".json");
        Files.writeString(tempFile, jsonContent);

        red.loadFromJson(tempFile.toString());

        // Verificar que las conexiones se cargaron en el grafo
        assertTrue(red.existeConexion("Ana", "Bruno"));
        assertTrue(red.existeConexion("Ana", "Carla"));
        assertTrue(red.existeConexion("Carla", "Diego"));

        // Verificar vecinos
        assertEquals(2, red.obtenerVecinos("Ana").size());
        assertEquals(1, red.obtenerVecinos("Bruno").size());
        assertEquals(2, red.obtenerVecinos("Carla").size());

        // Verificar distancias
        assertEquals(1, red.calcularDistancia("Ana", "Bruno"));
        assertEquals(2, red.calcularDistancia("Ana", "Diego")); // Ana -> Carla -> Diego
        assertEquals(3, red.calcularDistancia("Bruno", "Diego")); // Bruno -> Ana -> Carla -> Diego

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testLoadFromJson_conexionInexistenteIgnorada() throws Exception {
        // JSON con conexión a cliente que no existe (debe ignorarse con warning)
        String jsonContent = """
                {
                  "clientes": [
                    { "nombre": "A", "scoring": 10, "siguiendo": [], "conexiones": ["Fantasma"] }
                  ]
                }
                """;
        Path tempFile = Files.createTempFile("data_fantasma", ".json");
        Files.writeString(tempFile, jsonContent);

        // No debe lanzar excepción (política: ignorar con warning)
        red.loadFromJson(tempFile.toString());

        // El cliente A debe existir
        assertNotNull(red.buscarPorNombre("A"));

        // Pero no debe tener vecinos (la conexión fue ignorada)
        assertEquals(0, red.obtenerVecinos("A").size());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testGrafo_clienteAislado() {
        red.agregarCliente("Aislado", 50);
        red.agregarCliente("ConectadoA", 60);
        red.agregarCliente("ConectadoB", 70);

        red.agregarConexion("ConectadoA", "ConectadoB");

        // Cliente aislado tiene 0 vecinos
        assertEquals(0, red.obtenerVecinos("Aislado").size());

        // No hay camino entre aislado y los otros
        assertEquals(-1, red.calcularDistancia("Aislado", "ConectadoA"));
        assertEquals(-1, red.calcularDistancia("Aislado", "ConectadoB"));
    }

    @Test
    void testAgregarConexion_clienteInexistente() {
        red.agregarCliente("A", 10);

        // Intentar conectar con cliente que no existe
        assertThrows(IllegalArgumentException.class,
                () -> red.agregarConexion("A", "Inexistente"));
    }

    // ---------------- TESTS HISTORIAL PÚBLICO ----------------

    @Test
    void testGetHistorialAcciones_ordenCorrecto_masRecientePrimero() {
        // Ejecutar secuencia de acciones
        red.agregarCliente("ClienteA", 50);
        red.agregarCliente("ClienteB", 60);
        red.solicitarSeguir("ClienteA", "ClienteB");

        // Nota: procesarSiguienteSolicitud() y confirmarSeguimiento()
        // NO registran acciones en el historial

        // Obtener historial
        List<Action> historial = red.getHistorialAcciones();

        // Validar que no está vacío
        assertFalse(historial.isEmpty(), "El historial no debe estar vacío");
        assertEquals(3, historial.size(), "Debe haber exactamente 3 acciones registradas");

        // Validar orden: más reciente → más antiguo
        // [0] = REQUEST_FOLLOW (más reciente)
        // [1] = ADD_CLIENT ClienteB
        // [2] = ADD_CLIENT ClienteA (más antiguo)
        Action primera = historial.get(0);
        assertEquals("REQUEST_FOLLOW", primera.type().toString(),
                "La primera acción debe ser REQUEST_FOLLOW (la más reciente)");
        assertTrue(primera.detalle().contains("ClienteA") && primera.detalle().contains("ClienteB"));

        Action segunda = historial.get(1);
        assertEquals("ADD_CLIENT", segunda.type().toString());
        assertTrue(segunda.detalle().contains("ClienteB"));

        Action tercera = historial.get(2);
        assertEquals("ADD_CLIENT", tercera.type().toString());
        assertTrue(tercera.detalle().contains("ClienteA"));

        // Validar que todas tienen fechaHora no nulo
        for (Action a : historial) {
            assertNotNull(a.fechaHora(), "Cada acción debe tener fechaHora");
            assertNotNull(a.type(), "Cada acción debe tener tipo");
            assertNotNull(a.detalle(), "Cada acción debe tener detalle");
        }
    }

    @Test
    void testGetHistorialAcciones_listaInmutable() {
        // Ejecutar acciones
        red.agregarCliente("Test1", 10);
        red.agregarCliente("Test2", 20);

        // Obtener historial
        List<Action> historial = red.getHistorialAcciones();

        // Validar que es inmutable (no se puede modificar)
        assertThrows(UnsupportedOperationException.class,
                () -> historial.add(null),
                "La lista retornada debe ser inmutable");

        assertThrows(UnsupportedOperationException.class,
                () -> historial.remove(0),
                "No se debe poder remover elementos de la lista");

        assertThrows(UnsupportedOperationException.class,
                () -> historial.clear(),
                "No se debe poder limpiar la lista");
    }

    @Test
    void testGetHistorialAcciones_conLimit() {
        // Ejecutar múltiples acciones
        for (int i = 1; i <= 10; i++) {
            red.agregarCliente("Cliente" + i, i * 10);
        }

        // Pedir solo las últimas 5
        List<Action> historial5 = red.getHistorialAcciones(5);

        assertEquals(5, historial5.size(), "Debe retornar exactamente 5 acciones");

        // Validar que retorna las más recientes (Cliente10 debe ser la primera)
        Action primera = historial5.get(0);
        assertTrue(primera.detalle().contains("Cliente10"),
                "La primera acción debe ser la más reciente (Cliente10)");
    }

    @Test
    void testGetHistorialAcciones_limitNegativo_lanzaExcepcion() {
        red.agregarCliente("Test", 50);

        assertThrows(IllegalArgumentException.class,
                () -> red.getHistorialAcciones(-1),
                "Límite negativo debe lanzar IllegalArgumentException");
    }

    @Test
    void testGetHistorialAcciones_historialVacio() {
        // No ejecutar ninguna acción
        List<Action> historial = red.getHistorialAcciones();

        assertTrue(historial.isEmpty(), "El historial debe estar vacío inicialmente");
        assertEquals(0, historial.size());
    }
}
