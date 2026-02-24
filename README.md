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

## Runner de tests manuales (interactivo)

Para probar manualmente todas las funcionalidades del TP con un menú CLI interactivo:

```bash
mvn exec:java
```

**Características del runner:**

- ✅ Carga de JSON (demo.json o ruta personalizada)
- 📸 Snapshot completo del sistema en JSON pretty
- 📊 Consultas GET: clientes, scoring index, seguimientos, conexiones, ABB nivel 4
- 🧮 Calcular distancias con BFS
- ✏️ Operaciones POST: crear clientes, solicitar seguir, procesar solicitud, agregar conexión
- ↩️ Undo con reversión de efectos
- 📜 Historial de acciones

**Casos de prueba manuales:** Ver [`docs/MANUAL_TESTS.md`](docs/MANUAL_TESTS.md) para casos de prueba detallados.

### Ejecución rápida con carga automática

```bash
mvn exec:java -Dexec.args="demo.json"
```

Los tests (`RedSocialEmpresarialTest`) verifican carga de JSON, validaciones, FIFO y undo.

## Demo por consola

1. Compilar y copiar dependencias:
   ```bash
   mvn compile dependency:copy-dependencies
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
