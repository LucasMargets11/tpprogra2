# Análisis de Diseño e Implementación - Red Social Empresarial

**Trabajo Práctico - Algoritmos y Estructuras de Datos II**  
**Fecha:** 23 de febrero de 2026  
**Versión:** 1.0 - Implementación completa (3 iteraciones)

---

## Índice

1. [Respuestas a Preguntas del Enunciado](#1-respuestas-a-preguntas-del-enunciado)
2. [Tabla de Complejidades](#2-tabla-de-complejidades)
3. [Justificación de Estructuras Elegidas](#3-justificación-de-estructuras-elegidas)
4. [Análisis de Escalabilidad](#4-análisis-de-escalabilidad)
5. [Casos de Borde y Validaciones](#5-casos-de-borde-y-validaciones)
6. [Conclusiones](#6-conclusiones)

---

## 1. Respuestas a Preguntas del Enunciado

### Pregunta 1: ¿Qué estructuras de datos utilizaste y por qué?

#### Estructuras principales:

**1.1 HashMap (`clientesPorNombre`):**

- **Razón:** Búsqueda por nombre en O(1) promedio
- **Justificación:** El nombre es clave única y natural para identificar clientes
- **Alternativas rechazadas:**
  - Array/List: O(n) para búsquedas
  - ABB: O(log n), más lento y sin beneficio para búsqueda por nombre

**1.2 TreeMap (`indicePorScoring`):**

- **Razón:** Búsqueda por scoring exacto O(log n) + soporte de rangos O(log n + k)
- **Justificación:** Necesitamos buscar por scoring específico y rangos (min-max)
- **Alternativas rechazadas:**
  - HashMap: No soporta búsquedas por rango eficientemente
  - Array ordenado: Inserción O(n) vs O(log n) del TreeMap

**1.3 ArrayDeque (PILA para historial):**

- **Razón:** LIFO para undo, push/pop en O(1)
- **Justificación:** Necesitamos deshacer la última acción registrada
- **Alternativas rechazadas:**
  - Stack (legacy): ArrayDeque es más eficiente
  - LinkedList: Mayor overhead de memoria por nodos

**1.4 ArrayDeque (COLA para solicitudes):**

- **Razón:** FIFO para procesar solicitudes en orden, addLast/pollFirst O(1)
- **Justificación:** Requisito de procesamiento en orden de llegada
- **Alternativas rechazadas:**
  - LinkedList: Mayor overhead
  - PriorityQueue: No necesitamos prioridad, solo orden de inserción

**1.5 ArbolBinarioBusqueda (ABB propio):**

- **Razón:** Requisito del TP ("ABB visto en clase", no TreeMap)
- **Justificación:** Permite implementar operaciones custom como `obtenerNivel(n)`
- **Complejidad:** Inserción O(log n) promedio, O(n) peor caso (desbalanceado)
- **Uso específico:** Obtener nivel 4 para "ver quién tiene más seguidores"

**1.6 GrafoConexiones (Map + Set):**

- **Razón:** Representar conexiones bidireccionales con vecinos en O(1)
- **Justificación:** BFS para distancia en saltos requiere lista de adyacencias
- **Alternativas rechazadas:**
  - Matriz de adyacencias: O(V²) espacio, ineficiente para grafos dispersos
  - Lista de aristas: No permite obtener vecinos en O(1)

---

### Pregunta 2: ¿Cómo justificas la elección del scoring como clave de ordenamiento?

#### Justificación técnica:

**Requisito funcional:**

- Necesitamos buscar clientes por scoring exacto
- Necesitamos buscar clientes en rango de scoring (min-max)

**TreeMap como solución óptima:**

- Mantiene claves (scorings) ordenadas automáticamente
- `get(scoring)`: O(log n) para buscar scoring exacto
- `subMap(min, max)`: O(log n + k) para rango, siendo k = resultados

**Decisión de diseño:**

- **Clave primaria:** scoring (int)
- **Valor:** Set\<String\> con nombres de clientes
- **Justificación del Set:** Múltiples clientes pueden tener el mismo scoring

**Ventajas:**

1. Búsquedas eficientes sin recorrer toda la colección
2. Orden natural para reportes (clientes por scoring ascendente/descendente)
3. Operaciones de rango sin necesidad de filtrado manual

**Desventajas mitigadas:**

- TreeMap es más lento que HashMap (log n vs 1), pero necesitamos orden
- Espacio adicional del índice (O(n)), pero aceptable para funcionalidad requerida

---

### Pregunta 3: ¿Cómo representaste el TAD grafo y qué operaciones soporta?

#### Representación elegida:

**TAD GrafoConexiones:**

```java
Map<String, Set<String>> adyacencias
```

**Interpretación:**

- **Clave:** nombre del cliente (String)
- **Valor:** conjunto de vecinos/adyacentes (Set\<String\>)

**Tipo de grafo:**

- **No dirigido** (bidireccional): conexión es simétrica
- **No ponderado:** todas las aristas tienen peso 1 (para BFS)
- **Sin auto-loops:** cliente no puede conectarse consigo mismo
- **Sin aristas múltiples:** a lo sumo una conexión entre dos clientes

#### Operaciones implementadas:

**Básicas:**

1. `agregarConexion(u, v)` - O(1) promedio: agrega arista bidireccional
2. `vecinos(u)` - O(1) promedio: retorna Set\<String\> de vecinos
3. `existeConexion(u, v)` - O(1) promedio: verifica si hay arista

**Algoritmos:** 4. `calcularDistancia(origen, destino)` - O(V + E): BFS para camino mínimo

**Auxiliares:** 5. `agregarCliente(u)` - O(1): agrega vértice sin conexiones 6. `cantidadClientes()` - O(1): cantidad de vértices 7. `cantidadConexiones()` - O(V): cantidad de aristas (suma grados / 2) 8. `estaVacio()` - O(1): verifica si grafo está vacío 9. `clientes()` - O(1): retorna conjunto de todos los vértices

#### Algoritmo BFS para distancia:

**Pseudocódigo:**

```
calcularDistancia(origen, destino):
  si origen == destino: retornar 0

  cola ← Queue()
  visitados ← Set()
  distancias ← Map()

  cola.encolar(origen)
  visitados.agregar(origen)
  distancias[origen] ← 0

  mientras cola no vacía:
    actual ← cola.desencolar()

    para cada vecino de actual:
      si vecino no visitado:
        visitados.agregar(vecino)
        distancias[vecino] ← distancias[actual] + 1
        cola.encolar(vecino)

        si vecino == destino:
          retornar distancias[vecino]

  retornar -1  // No hay camino
```

**Complejidad:**

- **Temporal:** O(V + E) - cada vértice y arista se procesa una vez
- **Espacial:** O(V) - queue, visitados, distancias

**Correctitud:**

- BFS garantiza camino más corto en grafo no ponderado
- Si no hay camino, BFS termina sin encontrar destino → retorna -1

---

### Pregunta 4: ¿Cómo escala tu solución con muchos clientes?

#### Análisis de escalabilidad por operación:

**Operaciones críticas (uso frecuente):**

| Operación                         | Complejidad  | Escalabilidad | Observaciones                         |
| --------------------------------- | ------------ | ------------- | ------------------------------------- |
| `buscarPorNombre(nombre)`         | O(1)         | ✅ Excelente  | HashMap no depende de n               |
| `agregarCliente(nombre, scoring)` | O(log n)     | ✅ Muy buena  | TreeMap log, aceptable hasta millones |
| `buscarPorScoring(scoring)`       | O(log n + k) | ✅ Buena      | k = resultados, log despreciable      |
| `obtenerVecinos(cliente)`         | O(1)         | ✅ Excelente  | HashSet lookup inmediato              |
| `procesarSiguienteSolicitud()`    | O(1)         | ✅ Excelente  | Deque pollFirst constante             |

**Operaciones costosas (uso esporádico):**

| Operación                 | Complejidad | Escalabilidad | Mitigación                              |
| ------------------------- | ----------- | ------------- | --------------------------------------- |
| `undo() ADD_CLIENT`       | O(n)        | ⚠️ Aceptable  | Limpieza de referencias, poco frecuente |
| `calcularDistancia(u, v)` | O(V + E)    | ⚠️ Aceptable  | BFS completo, pero típico E = O(n)      |
| `loadFromJson(ruta)`      | O(n log n)  | ✅ Buena      | Carga única, n inserciones en TreeMap   |

#### Escenarios de escalabilidad:

**Escenario 1: Red pequeña (n < 1,000)**

- ✅ Todas las operaciones son instantáneas
- TreeMap, HashMap, Deque tienen overhead mínimo
- BFS encuentra caminos en microsegundos

**Escenario 2: Red mediana (n = 10,000 - 100,000)**

- ✅ TreeMap O(log n) sigue siendo rápido (log 100,000 ≈ 17 comparaciones)
- ✅ HashMap sigue siendo O(1) promedio con buen hash
- ⚠️ BFS puede tardar milisegundos en grafos densos
- ⚠️ Undo de ADD_CLIENT con muchas referencias podría ser lento

**Escenario 3: Red grande (n > 1,000,000)**

- ✅ HashMap sigue escalando bien (O(1) no cambia)
- ⚠️ TreeMap empieza a sentirse (log 1M ≈ 20, pero sigue aceptable)
- ⚠️ BFS en grafo denso (E ≈ n²) sería prohibitivo → requiere optimización
- ❌ Undo de ADD_CLIENT recorriendo 1M clientes sería lento

#### Mejoras para escalabilidad extrema:

**Si n > 1 millón:**

1. **Índice por scoring:**
   - Considerar índice secundario con particionamiento
   - Cachear búsquedas frecuentes

2. **Grafo:**
   - Índice de componentes conexas para evitar BFS innecesarios
   - Algoritmo bidireccional BFS (desde ambos extremos)
   - Precalcular distancias entre nodos "importantes"

3. **Historial:**
   - Limitar tamaño del historial (e.g., últimas 1000 acciones)
   - Undo solo deshace, no limpia referencias (lazy cleanup)

4. **Persistencia:**
   - Base de datos en lugar de JSON
   - Índices en BD para scoring, nombre

**Conclusión de escalabilidad:**

- **Hasta 100K clientes:** Solución actual es óptima
- **100K - 1M clientes:** Aceptable con hardware moderno
- **> 1M clientes:** Requiere optimizaciones (índices, particionamiento, BD)

---

## 2. Tabla de Complejidades

### 2.1 Complejidad Temporal

#### Operaciones de Cliente

| Operación                         | Mejor Caso | Caso Promedio | Peor Caso    | Justificación                     |
| --------------------------------- | ---------- | ------------- | ------------ | --------------------------------- |
| `crear()`                         | O(1)       | O(1)          | O(1)         | Inicialización de estructuras     |
| `agregarCliente(nombre, scoring)` | O(log n)   | O(log n)      | O(log n)     | TreeMap insert                    |
| `buscarPorNombre(nombre)`         | O(1)       | O(1)          | O(n)         | HashMap get (peor: colisiones)    |
| `buscarPorScoring(scoring)`       | O(log n)   | O(log n + k)  | O(log n + k) | TreeMap get + iterar k resultados |
| `buscarPorScoringEntre(min, max)` | O(log n)   | O(log n + m)  | O(log n + m) | TreeMap subMap + m resultados     |
| `cantidadClientes()`              | O(1)       | O(1)          | O(1)         | Map.size()                        |

#### Operaciones de Seguimiento

| Operación                        | Mejor Caso | Caso Promedio | Peor Caso | Justificación                  |
| -------------------------------- | ---------- | ------------- | --------- | ------------------------------ |
| `Cliente.seguirA(nombre)`        | O(1)       | O(1)          | O(n)      | HashSet add (peor: colisiones) |
| `Cliente.incrementarFollowers()` | O(1)       | O(1)          | O(1)      | Incremento de contador         |
| `confirmarSeguimiento(a, b)`     | O(1)       | O(1)          | O(n)      | seguirA + incrementar          |
| `solicitarSeguir(a, b)`          | O(1)       | O(1)          | O(1)      | Deque addLast                  |
| `procesarSiguienteSolicitud()`   | O(1)       | O(1)          | O(1)      | Deque pollFirst                |

#### Operaciones de Historial/Undo

| Operación               | Mejor Caso | Caso Promedio | Peor Caso | Justificación                    |
| ----------------------- | ---------- | ------------- | --------- | -------------------------------- |
| `undo()` REQUEST_FOLLOW | O(1)       | O(1)          | O(1)      | Deque removeLast                 |
| `undo()` ADD_CLIENT     | O(n)       | O(n)          | O(n)      | Limpia referencias en n clientes |

#### Operaciones de ABB

| Operación               | Mejor Caso | Caso Promedio  | Peor Caso  | Justificación                           |
| ----------------------- | ---------- | -------------- | ---------- | --------------------------------------- |
| `abb.insertar(cliente)` | O(1)       | O(log n)       | O(n)       | Raíz / balanceado / desbalanceado       |
| `abb.obtenerNivel(k)`   | O(1)       | O(n)           | O(n)       | BFS hasta nivel k (puede recorrer todo) |
| `abb.obtenerNivel4()`   | O(n)       | O(n + k log k) | O(n log n) | BFS + sort (k = clientes en nivel 4)    |
| `abb.inorder()`         | O(n)       | O(n)           | O(n)       | Recorrido completo                      |
| `abb.altura()`          | O(n)       | O(n)           | O(n)       | Recorrido recursivo                     |

#### Operaciones de Grafo

| Operación                       | Mejor Caso | Caso Promedio | Peor Caso | Justificación                        |
| ------------------------------- | ---------- | ------------- | --------- | ------------------------------------ |
| `grafo.agregarConexion(u, v)`   | O(1)       | O(1)          | O(V)      | HashMap + HashSet (peor: colisiones) |
| `grafo.vecinos(u)`              | O(1)       | O(1)          | O(1)      | HashMap get                          |
| `grafo.calcularDistancia(u, v)` | O(1)       | O(V + E)      | O(V + E)  | u==v / BFS completo / grafo denso    |
| `grafo.existeConexion(u, v)`    | O(1)       | O(1)          | O(1)      | HashSet contains                     |
| `grafo.cantidadClientes()`      | O(1)       | O(1)          | O(1)      | Map.size()                           |
| `grafo.cantidadConexiones()`    | O(V)       | O(V)          | O(V)      | Suma grados / 2                      |

#### Operación Compleja: loadFromJson

| Fase                          | Complejidad        | Justificación                                                 |
| ----------------------------- | ------------------ | ------------------------------------------------------------- |
| 1. Parsear JSON               | O(n)               | Gson deserializa n clientes                                   |
| 2. Insertar clientes          | O(n log n)         | n inserciones en TreeMap                                      |
| 3. Cargar seguimientos        | O(n · s)           | n clientes, s = seguimientos/cliente (máx 2) → O(n)           |
| 4. Actualizar followers       | O(n · s)           | n clientes, revisar seguimientos → O(n)                       |
| 5. Cargar conexiones en grafo | O(n · c)           | n clientes, c = conexiones/cliente → O(n + m), m = conexiones |
| **Total**                     | **O(n log n + m)** | Dominado por inserciones TreeMap + cargar conexiones          |

**Notación:**

- `n` = cantidad de clientes
- `k` = resultados de búsqueda por scoring
- `m` = rango de scoring o cantidad de conexiones
- `V` = cantidad de vértices (clientes en grafo)
- `E` = cantidad de aristas (conexiones en grafo)
- `s` = seguimientos por cliente (máximo 2)
- `c` = conexiones por cliente (variable)

---

### 2.2 Complejidad Espacial

#### Estructuras principales

| Estructura                    | Espacio  | Justificación                             |
| ----------------------------- | -------- | ----------------------------------------- |
| `clientesPorNombre` (HashMap) | O(n)     | n clientes                                |
| `indicePorScoring` (TreeMap)  | O(n)     | n nombres distribuidos en sets            |
| `historial` (Deque)           | O(h)     | h = acciones registradas (sin límite)     |
| `colaSeguimientos` (Deque)    | O(s)     | s = solicitudes pendientes                |
| `abb` (ArbolBinarioBusqueda)  | O(n)     | n nodos con clientes                      |
| `grafo` (GrafoConexiones)     | O(V + E) | V vértices + E aristas (x2 bidireccional) |

#### Cliente individual

| Atributo               | Espacio      | Justificación                   |
| ---------------------- | ------------ | ------------------------------- |
| `nombre` (String)      | O(L)         | L = longitud del nombre         |
| `scoring` (int)        | O(1)         | Primitivo                       |
| `siguiendo` (Set)      | O(f)         | f = seguimientos (máx 2) → O(1) |
| `conexiones` (Set)     | O(c)         | c = conexiones del cliente      |
| `followersCount` (int) | O(1)         | Primitivo                       |
| **Total por cliente**  | **O(L + c)** | Nombre + conexiones             |

#### Espacio total del sistema

```
Espacio = O(n) [HashMap]
        + O(n) [TreeMap]
        + O(h) [historial]
        + O(s) [solicitudes]
        + O(n) [ABB]
        + O(V + E) [grafo]
        + O(n · (L + c)) [datos de clientes]

Espacio = O(n · L + n · c + h + s + E)
```

**Simplificado:**

- Asumiendo L constante (nombres cortos): O(n · c + h + s + E)
- Si c es constante (pocas conexiones): O(n + h + s + E)
- Si h y s están acotados: **O(n + E)**

**En el peor caso:**

- Grafo completo: E = O(n²) → Espacio total O(n²)
- Historial ilimitado: h → ∞ → Crece sin límite

**En el caso típico:**

- Grafo disperso: E = O(n) → Espacio O(n)
- Historial acotado (e.g., últimas 1000 acciones): h = O(1) → Espacio O(n)

---

## 3. Justificación de Estructuras Elegidas

### 3.1 HashMap vs TreeMap vs Array

**Para búsqueda por nombre: HashMap**

| Criterio       | HashMap | TreeMap  | Array/List                 |
| -------------- | ------- | -------- | -------------------------- |
| Búsqueda       | O(1) ✅ | O(log n) | O(n)                       |
| Inserción      | O(1) ✅ | O(log n) | O(1) append, O(n) ordenado |
| Orden          | ❌ No   | ✅ Sí    | ✅ Sí (si se mantiene)     |
| Uso de memoria | Media   | Media+   | Baja                       |

**Decisión:** HashMap porque la búsqueda por nombre es O(1) y no necesitamos orden por nombre.

**Para búsqueda por scoring: TreeMap**

| Criterio         | HashMap | TreeMap         | Array ordenado           |
| ---------------- | ------- | --------------- | ------------------------ |
| Búsqueda exacta  | O(1) ✅ | O(log n)        | O(log n) binary search   |
| Búsqueda rango   | ❌ O(n) | ✅ O(log n + k) | O(log n + k)             |
| Inserción        | O(1)    | O(log n) ✅     | O(n)                     |
| Orden automático | ❌ No   | ✅ Sí           | ✅ Sí (costoso mantener) |

**Decisión:** TreeMap porque soporta búsquedas por rango eficientemente y mantiene orden automático.

---

### 3.2 ArrayDeque vs LinkedList vs Stack

**Para historial (PILA) y cola (FIFO): ArrayDeque**

| Criterio                 | ArrayDeque  | LinkedList       | Stack (legacy)     |
| ------------------------ | ----------- | ---------------- | ------------------ |
| push/pop (LIFO)          | O(1) ✅     | O(1) ✅          | O(1)               |
| addLast/pollFirst (FIFO) | O(1) ✅     | O(1) ✅          | ❌ No soporta FIFO |
| Memoria                  | Contigua ✅ | Nodos (overhead) | Contigua           |
| Cache locality           | ✅ Mejor    | ❌ Peor          | ✅ Mejor           |
| API moderna              | ✅ Sí       | ✅ Sí            | ❌ Deprecated      |

**Decisión:** ArrayDeque es más eficiente que LinkedList (cache locality) y más moderno que Stack.

---

### 3.3 ABB propio vs TreeMap

**Para nivel 4 del ABB: ABB propio**

| Criterio         | ABB propio            | TreeMap                     |
| ---------------- | --------------------- | --------------------------- |
| Control total    | ✅ Sí                 | ❌ No (black box)           |
| obtenerNivel(k)  | ✅ Implementable      | ❌ No disponible            |
| Requisito del TP | ✅ Cumple             | ❌ No aceptado              |
| Complejidad      | O(log n) insert       | O(log n) insert (Red-Black) |
| Balanceo         | ❌ No (desbalanceado) | ✅ Sí (auto-balanceo)       |

**Decisión:** ABB propio porque el TP requiere "ABB visto en clase" y necesitamos `obtenerNivel(k)` custom.

---

### 3.4 Lista de adyacencias vs Matriz de adyacencias

**Para grafo: Map<String, Set<String>> (lista de adyacencias)**

| Criterio            | Lista adyacencias     | Matriz adyacencias     |
| ------------------- | --------------------- | ---------------------- |
| Espacio             | O(V + E) ✅           | O(V²)                  |
| Vecinos de u        | O(1) ✅               | O(V)                   |
| Existe arista (u,v) | O(1) (con HashSet) ✅ | O(1) ✅                |
| Agregar arista      | O(1) ✅               | O(1)                   |
| Grafos dispersos    | ✅ Eficiente          | ❌ Desperdicia espacio |
| Grafos densos       | Aceptable             | ✅ Mejor               |

**Decisión:** Lista de adyacencias porque típicamente E << V² (grafo disperso) y permite obtener vecinos en O(1).

---

## 4. Análisis de Escalabilidad

### 4.1 Benchmarks Teóricos

**Asumiendo:**

- Hardware moderno: 1M operaciones HashMap/s, 500K operaciones TreeMap/s
- Grafo disperso: cada cliente tiene ~10 conexiones en promedio (E ≈ 10n)

| Operación                          | n = 1K | n = 10K | n = 100K | n = 1M |
| ---------------------------------- | ------ | ------- | -------- | ------ |
| `buscarPorNombre`                  | < 1 µs | < 1 µs  | < 1 µs   | < 1 µs |
| `agregarCliente`                   | 10 µs  | 13 µs   | 17 µs    | 20 µs  |
| `buscarPorScoring` (10 resultados) | 10 µs  | 13 µs   | 17 µs    | 20 µs  |
| `calcularDistancia` (E ≈ 10n)      | 100 µs | 1 ms    | 10 ms    | 100 ms |
| `undo ADD_CLIENT`                  | 100 µs | 1 ms    | 10 ms    | 100 ms |
| `loadFromJson`                     | 10 ms  | 130 ms  | 1.7 s    | 20 s   |

**Interpretación:**

- ✅ Búsquedas por nombre escalan perfectamente (O(1))
- ✅ Operaciones de TreeMap son rápidas hasta 1M elementos
- ⚠️ BFS y undo se vuelven perceptibles en redes grandes
- ⚠️ Carga de JSON > 100K clientes requiere optimización o async

---

### 4.2 Cuellos de Botella Identificados

**1. Undo de ADD_CLIENT: O(n)**

- **Problema:** Recorre todos los clientes para limpiar referencias
- **Impacto:** Lento si n > 100K
- **Solución:** Lazy cleanup o limitar historial

**2. BFS en grafo denso: O(V + E)**

- **Problema:** Si E ≈ V² (grafo casi completo), BFS es O(V²)
- **Impacto:** Distancia tarda > 1s en grafos > 10K nodos densos
- **Solución:** Bidirectional BFS, precalcular distancias frecuentes

**3. Carga de JSON: O(n log n)**

- **Problema:** Inserción en TreeMap domina el tiempo
- **Impacto:** Carga > 100K clientes tarda > 1s
- **Solución:** Bulk loading, parseo async, Base de Datos

---

### 4.3 Estrategias de Optimización Futuras

**Para n > 1M:**

1. **Particionamiento del índice de scoring:**
   - Dividir TreeMap en ranges (0-10, 11-20, ..., 91-100)
   - Búsqueda paralela en particiones

2. **Caché de consultas frecuentes:**
   - LRU cache para búsquedas repetidas
   - Precalcular estadísticas (top 10 scoring, etc.)

3. **Grafo distribuido:**
   - Particionar grafo por comunidades
   - Calcular distancia intra-partición primero

4. **Base de Datos:**
   - PostgreSQL con índices en nombre y scoring
   - Apache Cassandra para grafos masivos

---

## 5. Casos de Borde y Validaciones

### 5.1 Validaciones Implementadas

#### Cliente

- ✅ Nombre != null && !isBlank()
- ✅ Scoring >= 0
- ✅ No seguirse a sí mismo
- ✅ No duplicar seguimiento
- ✅ Máximo 2 seguimientos

#### JSON

- ✅ Archivo existe
- ✅ JSON bien formado
- ✅ Sin clientes duplicados
- ✅ Scoring no negativo
- ✅ Máximo 2 seguimientos por cliente

#### Grafo

- ✅ No auto-loops (u == u)
- ✅ Validar que clientes existen antes de conectar
- ✅ Conexiones bidireccionales automáticas

---

### 5.2 Casos de Borde Cubiertos (40 tests)

#### Entradas vacías/nulas

- ✅ JSON vacío (0 clientes) → comportamiento estable
- ✅ Cola vacía al procesar solicitud → retorna null
- ✅ Historial vacío al hacer undo → retorna Optional.empty()
- ✅ Cliente sin conexiones al pedir vecinos → Set vacío
- ✅ Nombre vacío/null al agregar cliente → IllegalArgumentException

#### Límites y restricciones

- ✅ Scoring = 0 (mínimo válido) → aceptado
- ✅ Intentar 3er seguimiento → IllegalStateException
- ✅ JSON con cliente con >2 seguimientos → IllegalArgumentException
- ✅ Auto-seguimiento → IllegalArgumentException
- ✅ Duplicar seguimiento → IllegalArgumentException

#### Grafos especiales

- ✅ Cliente aislado (sin conexiones) → distancia -1 a cualquier otro
- ✅ Grafo desconectado (componentes separadas) → distancia -1
- ✅ Grafo con ciclos → BFS encuentra camino mínimo
- ✅ Distancia 0 (mismo cliente) → retorna 0 sin BFS

#### Consistencia

- ✅ Undo ADD_CLIENT limpia referencias en otros clientes
- ✅ Undo REQUEST_FOLLOW elimina de cola FIFO
- ✅ loadFromJson actualiza followersCount correctamente
- ✅ Conexiones bidireccionales en grafo (u-v ⟺ v-u)

---

## 6. Conclusiones

### 6.1 Cumplimiento de Requisitos

**Iteración 1 (Funcionalidad Base):**

- ✅ Búsquedas por nombre y scoring implementadas
- ✅ Historial con undo funcional
- ✅ Cola FIFO para solicitudes
- ✅ Carga desde JSON con validaciones
- ✅ 20 tests pasando

**Iteración 2 (Seguimientos + ABB):**

- ✅ Validación máximo 2 seguimientos
- ✅ ABB propio con inserción y obtenerNivel(k)
- ✅ followersCount sincronizado
- ✅ Nivel 4 del ABB con ordenamiento por seguidores
- ✅ 29 tests pasando (+9)

**Iteración 3 (Grafo + BFS):**

- ✅ Grafo de conexiones bidireccionales
- ✅ BFS para distancia en saltos
- ✅ Operación vecinos(u) en O(1)
- ✅ Integración con loadFromJson
- ✅ 40 tests pasando (+11)

---

### 6.2 Fortalezas de la Solución

1. **Eficiencia:** Operaciones críticas en O(1) o O(log n)
2. **Escalabilidad:** Funciona bien hasta 100K clientes
3. **Correctitud:** 40 tests deterministas cubriendo casos borde
4. **Modularidad:** Separación clara TAD Cliente / ABB / Grafo / RedSocial
5. **Documentación:** TAD_IR.md completo con invariantes y complejidades

---

### 6.3 Limitaciones y Mejoras Futuras

**Limitaciones actuales:**

- ABB sin auto-balanceo (puede degenerar en O(n))
- Historial ilimitado (crece indefinidamente)
- Undo ADD_CLIENT en O(n) (lento en redes grandes)
- Sin persistencia más allá de JSON

**Mejoras recomendadas:**

1. Implementar AVL o Red-Black para ABB balanceado
2. Limitar historial a últimas N acciones
3. Lazy cleanup en undo (marcar como "deleted" sin recorrer)
4. Migrar a Base de Datos para persistencia escalable
5. API REST para integración con frontend

---

### 6.4 Aprendizajes Clave

**Estructuras de Datos:**

- HashMap + TreeMap son complementarios, no excluyentes
- ABB custom permite operaciones no disponibles en TreeMap
- Lista de adyacencias es óptima para grafos dispersos

**Algoritmos:**

- BFS es el algoritmo correcto para distancia en grafos no ponderados
- Undo requiere diseño cuidadoso para mantener consistencia
- Validaciones tempranas evitan estados inconsistentes

**Ingeniería de Software:**

- Tests deterministas son esenciales para validar corrección
- Documentación de invariantes ayuda a encontrar bugs
- Separación de responsabilidades facilita testing y mantenimiento

---

**Implementación completa: 40/40 tests pasando ✅**

**Archivos:**

- Código: `src/main/java/ar/uade/redsocial/`
- Tests: `src/test/java/ar/uade/redsocial/RedSocialEmpresarialTest.java`
- Documentación: `docs/TAD_IR.md`, `docs/ANALISIS.md`, `docs/STATUS.md`

---

**Fecha de entrega:** 23 de febrero de 2026  
**Estado:** ✅ Completo y validado
