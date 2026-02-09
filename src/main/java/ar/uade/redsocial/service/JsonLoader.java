package ar.uade.redsocial.service;

import ar.uade.redsocial.dto.ClienteDTO;
import ar.uade.redsocial.dto.RedDTO;
import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Carga datos iniciales desde un archivo JSON.
 */
public class JsonLoader {

    private final Gson gson = new Gson();

    public void cargarDesdeArchivo(String path, RedSocialEmpresarial sistema) throws IOException {
        try (Reader reader = new FileReader(path)) {
            RedDTO red = gson.fromJson(reader, RedDTO.class);

            if (red == null || red.clientes == null) return;

            for (ClienteDTO c : red.clientes) {
                sistema.agregarCliente(c.nombre, c.scoring);
            }
        }
    }
}
