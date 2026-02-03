package ar.uade.redsocial;

import org.junit.jupiter.api.Test;

import ar.uade.redsocial.service.RedSocialEmpresarial;

import static org.junit.jupiter.api.Assertions.*;

public class RedSocialEmpresarialTest {

    @Test
    void agregarYBuscarCliente() {
        RedSocialEmpresarial sistema = new RedSocialEmpresarial();
        sistema.agregarCliente("Alice", 95);

        assertNotNull(sistema.buscarPorNombre("Alice"));
        assertEquals(95, sistema.buscarPorNombre("Alice").getScoring());
    }

    @Test
    void buscarPorScoring() {
        RedSocialEmpresarial sistema = new RedSocialEmpresarial();
        sistema.agregarCliente("Alice", 95);
        sistema.agregarCliente("Bob", 88);
        sistema.agregarCliente("Charlie", 95);

        assertEquals(2, sistema.buscarPorScoring(95).size());
        assertEquals(1, sistema.buscarPorScoring(88).size());
    }

    @Test
    void deshacerAccion() {
        RedSocialEmpresarial sistema = new RedSocialEmpresarial();
        sistema.agregarCliente("Alice", 95);
        assertEquals(1, sistema.cantidadClientes());

        sistema.deshacerUltimaAccion();
        assertEquals(0, sistema.cantidadClientes());
    }

    @Test
    void colaSeguimientosFIFO() {
        RedSocialEmpresarial sistema = new RedSocialEmpresarial();
        sistema.agregarCliente("Alice", 95);
        sistema.agregarCliente("Bob", 88);
        sistema.agregarCliente("Charlie", 80);

        sistema.solicitarSeguir("Alice", "Bob");
        sistema.solicitarSeguir("Alice", "Charlie");

        assertEquals("Bob", sistema.procesarSiguienteSolicitud().objetivo());
        assertEquals("Charlie", sistema.procesarSiguienteSolicitud().objetivo());
    }
}
