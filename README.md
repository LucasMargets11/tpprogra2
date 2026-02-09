# Red Social Empresarial

Pequeño proyecto Maven que implementa las operaciones básicas de una red social corporativa para clientes. Incluye estructuras de datos para búsquedas por nombre, índices por scoring, historial de acciones y cola FIFO de solicitudes de seguimiento.

## Requisitos

- JDK 21 o superior
- Apache Maven 3.9+

## Ejecutar pruebas automatizadas

```bash
mvn clean test
```

Los tests (`RedSocialEmpresarialTest`) verifican carga de JSON, validaciones, FIFO y undo.

## Demo por consola

1. Empaquetar y copiar dependencias:
   ```bash
   mvn -DskipTests package dependency:copy-dependencies
   ```
2. Ejecutar la demo (ruta del JSON opcional, por defecto `demo.json` en la raíz):
   - **Windows PowerShell**
     ```powershell
     java -cp "target\classes;target\dependency\*" ar.uade.redsocial.DemoApp demo.json
     ```
   - **Linux / macOS**
     ```bash
     java -cp "target/classes:target/dependency/*" ar.uade.redsocial.DemoApp demo.json
     ```

La salida muestra:

- Archivo cargado y cantidad total de clientes.
- Clientes en el rango de scoring 0-100.
- Resultado de `buscarPorNombre` (priorizando "Ana").
- Ejemplo de solicitudes de seguimiento procesadas en FIFO.
- Demostración de `undo()` revirtiendo un alta temporal.

## Carga de datos

El archivo `demo.json` incluido sigue la estructura esperada por los DTOs (`ClienteDTO`, `RedDTO`). Puedes aportar tu propio archivo JSON respetando el mismo formato y pasarlo como primer argumento al ejecutar `DemoApp`.
