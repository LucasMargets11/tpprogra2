# Estado Actual del Proyecto - Red Social Empresarial

**Fecha de Auditoría:** 23 de febrero de 2026

## 1. Estructura de Paquetes y Clases Existentes

### Paquetes principales:

- `ar.uade.redsocial` (package raíz)
- `ar.uade.redsocial.model` (entidades del dominio)
- `ar.uade.redsocial.service` (lógica de negocio)
- `ar.uade.redsocial.dto` (objetos de transferencia de datos)

### Clases existentes:

#### Modelo (`ar.uade.redsocial.model`)

- **Cliente**: Entidad principal con nombre, scoring, siguiendo[], conexiones[]
- **Action**: Record para historial de acciones (type, detalle, payload, fechaHora)
- **ActionType**: Enum (ADD_CLIENT, REQUEST_FOLLOW, PROCESS_FOLLOW)
- **FollowRequest**: Record para solicitudes de seguimiento (solicitante, objetivo, fechaHora)

#### Servicio (`ar.uade.redsocial.service`)

- **RedSocialEmpresarial**: TAD principal (ver detalles abajo)
- **JsonLoader**: (probablemente obsoleto, no usado actualmente)

#### DTO (`ar.uade.redsocial.dto`)

- **ClienteDTO**: Para deserialización JSON
- **RedDTO**: (uso pendiente de verificar)

## 2. TAD Principal: RedSocialEmpresarial

### Estructuras internas:

```java
Map<String, Cliente> clientesPorNombre          // HashMap - busqueda O(1)
NavigableMap<Integer, Set<String>> indicePorScoring  // TreeMap - busqueda O(log n)
Deque<Action> historial                         // PILA para undo
Deque<FollowRequest> colaSeguimientos           // COLA FIFO
```

### Operaciones implementadas:

- ✅ `loadFromJson(String ruta)`: Carga datos desde JSON
- ✅ `agregarCliente(String, int)`: Agrega cliente y registra acción
- ✅ `buscarPorNombre(String)`: O(1) promedio
- ✅ `buscarPorScoring(int)`: O(log n + k)
- ✅ `buscarPorScoringEntre(int, int)`: Búsqueda por rango
- ✅ `solicitarSeguir(String, String)`: Agrega a cola FIFO
- ✅ `procesarSiguienteSolicitud()`: FIFO
- ✅ `undo()`: Deshace última acción (ADD_CLIENT, REQUEST_FOLLOW)

## 3. Representación de Seguidos y Conexiones

### En la clase Cliente:

```java
Set<String> siguiendo   // Nombres de clientes que este cliente sigue
Set<String> conexiones  // Nombres de clientes con los que tiene conexión
```

### Estado actual:

- ✅ Se cargan desde JSON correctamente
- ❌ **NO** hay validación de máximo 2 seguimientos
- ❌ **NO** hay validación de no seguirse a sí mismo
- ❌ **NO** hay validación de seguimientos duplicados
- ❌ Conexiones no están centralizadas en un Grafo

## 4. Carga desde JSON (loadFromJson)

### Estado:

- ✅ Implementado y funcional
- ✅ Formato soportado: `{ "clientes": [ {nombre, scoring, siguiendo[], conexiones[]} ] }`
- ✅ Valida:
  - Archivo existente
  - JSON bien formado (JsonSyntaxException)
  - Clientes duplicados
  - Scoring negativo
- ⚠️ **NO valida** máximo 2 seguimientos
- ⚠️ Parsea siguiendo y conexiones **sin restricciones**

### Problema identificado:

**demo.json contiene datos inválidos:**

```json
{
  "nombre": "Gisela",
  "siguiendo": ["Ana", "Bruno", "Carla"] // ❌ 3 seguimientos (debe ser máx 2)
}
```

**Acción requerida:** Corregir demo.json o rechazar en carga con validación.

## 5. Historial y Cola de Solicitudes

### Historial (PILA - LIFO):

- ✅ Implementado con `Deque<Action>`
- ✅ Registra: ADD_CLIENT, REQUEST_FOLLOW
- ✅ Undo funcional con limpieza de Referencias

### Cola de Solicitudes (FIFO):

- ✅ Implementada con `Deque<FollowRequest>`
- ✅ FIFO correcto: `addLast()` + `pollFirst()`
- ✅ Integración con undo para deshacer REQUEST_FOLLOW

## 6. Tests Existentes

### Archivo: `RedSocialEmpresarialTest.java`

**Tests implementados (~10 tests):**

- ✅ testLoadFromJson_ok_conFormatoDelTP
- ✅ testLoadFromJson_jsonInvalido_falla
- ✅ testLoadFromJson_duplicados_falla
- ✅ testLoadFromJson_scoringNegativo_falla
- ✅ testFIFO_solicitarYProcesar
- ✅ testUndo_RequestFollow
- ✅ testUndo_AddClient_CleanReferences
- ✅ testBuscadores

**Tests faltantes para Iteración 1:**

- ❌ testLoadFromJson_jsonVacio (debe ser estable)
- ❌ testAgregarCliente_nombreVacio (debe rechazar)
- ❌ testBuscarPorNombre_inexistente (debe retornar null)
- ❌ testUndo_sinAcciones (Optional vacío)
- ❌ testProcesarSolicitud_colaVacia (debe retornar null)

## 7. Configuración Maven (pom.xml)

### Estado:

- ✅ JUnit 5 (org.junit.jupiter:junit-jupiter 5.10.2)
- ✅ Gson 2.11.0
- ✅ maven-surefire-plugin 3.2.5 configurado
- ✅ Java 21 (LTS)

## 8. Faltantes por Iteración

### Iteración 1 (Funcionalidad Base):

- ❌ 5 tests adicionales (ver punto 6)
- ❌ `docs/TAD_IR.md` con invariantes y complejidad

### Iteración 2 (Seguimientos + ABB):

- ❌ Validación máximo 2 seguimientos
- ❌ Validación no seguirse a sí mismo
- ❌ Validación no duplicar seguimiento
- ❌ Contador `followersCount` por cliente
- ❌ Estructura `ArbolBinarioBusqueda` propia (no TreeMap)
- ❌ Método `obtenerNivel(int nivel)` con BFS
- ❌ Método `obtenerNivel4()` para mostrar nivel 4
- ❌ Tests de ABB, seguimientos, nivel 4
- ❌ Actualizar `docs/TAD_IR.md` con Iteración 2

### Iteración 3 (Grafo + BFS):

- ❌ Clase `GrafoConexiones` con `Map<String, Set<String>>`
- ❌ Método `agregarConexion(u, v)` bidireccional
- ❌ Método `vecinos(u)` → Set<String>
- ❌ Método `calcularDistancia(origen, destino)` con BFS
- ❌ Integrar grafo con carga JSON
- ❌ Tests de grafo, vecinos, BFS, distancia
- ❌ Actualizar `docs/TAD_IR.md` con Iteración 3

### Documentación Final:

- ❌ `docs/ANALISIS.md` con:
  - Respuesta a 4 preguntas del TP
  - Tabla de complejidades temporal/espacial
  - Justificación de decisiones de diseño

## 9. Decisiones de Diseño Actuales

### Positivas:

- ✅ Uso de HashMap para búsqueda O(1) por nombre
- ✅ Uso de TreeMap para índice ordenado por scoring
- ✅ Undo con limpieza de referencias (evita inconsistencias)
- ✅ Separación clara entre Cliente (modelo) y RedSocialEmpresarial (TAD)
- ✅ Uso de records para Action y FollowRequest (inmutabilidad)

### A mejorar:

- ⚠️ Cliente expone `seguirA()` y `agregarConexion()` públicamente
  - Debería ser responsabilidad del TAD (encapsulación)
- ⚠️ Conexiones no están en un Grafo centralizado
- ⚠️ No hay contador de seguidores (followersCount)

## 10. Próximos Pasos

1. ✅ **Completar Iteración 1**: Tests faltantes + `docs/TAD_IR.md`
2. ⬜ **Corregir demo.json**: Gisela debe tener máx 2 siguiendo
3. ⬜ **Iteración 2**: Validaciones + ABB propio + nivel 4
4. ⬜ **Iteración 3**: Grafo + BFS
5. ⬜ **Documentación final**: `docs/ANALISIS.md`

---

**Observaciones finales:**

- El código actual es sólido y bien estructurado
- La base para Iteraciones 2 y 3 está preparada
- Priorizar: validaciones de negocio → ABB propio → Grafo + BFS
- Mantener la calidad de tests (deterministas, casos borde)
