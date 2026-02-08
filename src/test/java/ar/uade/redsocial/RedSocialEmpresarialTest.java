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
        // Pero primero vamos a re-agregar B para probar el caso inverso (Borrar A cuando B lo sigue)
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
}
