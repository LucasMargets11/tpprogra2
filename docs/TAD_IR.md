# TAD e Invariantes de Representación - Red Social Empresarial

## Índice

1. [Iteración 1 - Funcionalidad Base](#iteración-1---funcionalidad-base)
2. [Iteración 2 - Seguimientos y ABB](#iteración-2---seguimientos-y-abb)
3. [Iteración 3 - Grafo y Distancias](#iteración-3---grafo-y-distancias)

---

## Iteración 1 - Funcionalidad Base

### 1.1 TAD RedSocialEmpresarial

#### Estructuras de Representación

```java
rep RedSocialEmpresarial {
    Map<String, Cliente> clientesPorNombre;              // HashMap
    NavigableMap<Integer, Set<String>> indicePorScoring; // TreeMap
    Deque<Action> historial;                             // ArrayDeque (PILA)
    Deque<FollowRequest> colaSeguimientos;               // ArrayDeque (COLA)
}
```

#### Operaciones

##### Inicialización

```
crear() → RedSocialEmpresarial
  Pre: -
  Post: crea una red social vacía con todas las estructuras inicializadas
  Complejidad: O(1)
```

##### Carga de Datos

```
loadFromJson(String ruta) → void
  Pre: ruta != null && archivo existe
  Post: carga clientes desde JSON con formato {"clientes": [...]}
        lanza IllegalArgumentException si:
        - archivo no existe
        - JSON mal formado
        - cliente duplicado
        - scoring < 0
  Complejidad: O(n) siendo n = cantidad de clientes en JSON
  Nota: NO registra en historial
```

##### Gestión de Clientes

```
agregarCliente(String nombre, int scoring) → void
  Pre: nombre != null && !nombre.isBlank() && scoring >= 0
       && !existeCliente(nombre)
  Post: agrega cliente al sistema
        actualiza clientesPorNombre
        actualiza indicePorScoring
        registra acción ADD_CLIENT en historial
  Complejidad: O(1) promedio (HashMap) + O(log n) (TreeMap)
             = O(log n) total

buscarPorNombre(String nombre) → Cliente | null
  Pre: nombre != null
  Post: retorna el cliente con ese nombre o null si no existe
  Complejidad: O(1) promedio

buscarPorScoring(int scoring) → List<Cliente>
  Pre: -
  Post: retorna lista de clientes con ese scoring exacto
        lista vacía si no hay ninguno
  Complejidad: O(log n + k) siendo:
               - n = total de clientes
               - k = cantidad de clientes con ese scoring

buscarPorScoringEntre(int min, int max) → List<Cliente>
  Pre: min <= max
  Post: retorna lista de clientes con scoring en [min, max] (inclusivo)
  Complejidad: O(log n + m) siendo:
               - n = total de clientes
               - m = cantidad de clientes en el rango

cantidadClientes() → int
  Pre: -
  Post: retorna cantidad total de clientes
  Complejidad: O(1)
```

##### Historial y Undo

```
undo() → Optional<Action>
  Pre: -
  Post: deshace la última acción registrada
        retorna Optional.empty() si no hay acciones
        tipos de acción:
        - ADD_CLIENT: elimina cliente + limpia referencias
        - REQUEST_FOLLOW: elimina solicitud de cola
  Complejidad:
    - ADD_CLIENT: O(n) por limpieza de referencias
    - REQUEST_FOLLOW: O(1)
```

##### Cola de Solicitudes de Seguimiento

```
solicitarSeguir(String solicitante, String objetivo) → void
  Pre: solicitante != null && objetivo != null
       && existeCliente(solicitante) && existeCliente(objetivo)
  Post: agrega solicitud a cola FIFO
        registra acción REQUEST_FOLLOW en historial
  Complejidad: O(1)

procesarSiguienteSolicitud() → FollowRequest | null
  Pre: -
  Post: retorna y elimina la solicitud más antigua (FIFO)
        retorna null si cola vacía
  Complejidad: O(1)

cantidadSolicitudesPendientes() → int
  Pre: -
  Post: retorna cantidad de solicitudes en cola
  Complejidad: O(1)
```

---

### 1.2 TAD Cliente

#### Representación

```java
rep Cliente {
    String nombre;              // inmutable
    int scoring;                // inmutable
    Set<String> siguiendo;      // HashSet (preparado para Iteración 2)
    Set<String> conexiones;     // HashSet (preparado para Iteración 3)
}
```

#### Operaciones Públicas

```
crear(String nombre, int scoring) → Cliente
  Pre: nombre != null && !nombre.isBlank() && scoring >= 0
  Post: crea un cliente con siguiendo y conexiones vacíos

getNombre() → String
  Complejidad: O(1)

getScoring() → int
  Complejidad: O(1)

getSiguiendo() → Set<String>
  Post: retorna vista inmutable
  Complejidad: O(1)

getConexiones() → Set<String>
  Post: retorna vista inmutable
  Complejidad: O(1)
```

#### Operaciones Internas (uso en iteraciones 2/3)

```
seguirA(String nombreCliente) → void
dejarDeSeguir(String nombreCliente) → void
agregarConexion(String nombreCliente) → void
removerConexion(String nombreCliente) → void
```

---

### 1.3 Invariantes de Representación (IR)

#### IR1: Consistencia nombre-cliente

```
∀ nombre ∈ clientesPorNombre.keys() ⇒
    clientesPorNombre.get(nombre).getNombre() == nombre
```

**Verificación:** Cada entrada del mapa nombre → cliente tiene concordancia entre clave y atributo.

#### IR2: Sincronización índice por scoring

```
∀ cliente ∈ clientesPorNombre.values() ⇒
    cliente.getNombre() ∈ indicePorScoring.get(cliente.getScoring())
```

**Verificación:** Todo cliente está registrado en el índice por scoring con su scoring correspondiente.

#### IR3: Unicidad de nombres

```
|clientesPorNombre.keys()| == cantidad de clientes únicos
∀ c1, c2 ∈ clientesPorNombre.values(), c1 ≠ c2 ⇒ c1.getNombre() ≠ c2.getNombre()
```

**Verificación:** No hay clientes duplicados por nombre.

#### IR4: Scoring no negativo

```
∀ cliente ∈ clientesPorNombre.values() ⇒ cliente.getScoring() >= 0
```

**Verificación:** Todo scoring es mayor o igual a cero.

#### IR5: Nombre válido

```
∀ cliente ∈ clientesPorNombre.values() ⇒
    cliente.getNombre() != null && !cliente.getNombre().isBlank()
```

**Verificación:** Todo cliente tiene nombre no vacío.

#### IR6: Historial-acción consistente con estado

```
∀ action ∈ historial donde action.type == ADD_CLIENT ⇒
    action.detalle ∈ clientesPorNombre.keys() ∨ acción ya fue deshecha
```

**Verificación:** Las acciones del historial se corresponden con el estado actual o fueron deshecha

#### IR7: Cola FIFO de solicitudes válidas

```
∀ request ∈ colaSeguimientos ⇒
    request.solicitante() ∈ clientesPorNombre.keys() ∧
    request.objetivo() ∈ clientesPorNombre.keys()
```

**Verificación:** Todas las solicitudes referencian clientes existentes (pueden volverse inválidas si se deshace ADD_CLIENT).

#### IR8: Índice por scoring no tiene sets vacíos

```
∀ scoring ∈ indicePorScoring.keys() ⇒
    indicePorScoring.get(scoring) != null &&
    !indicePorScoring.get(scoring).isEmpty()
```

**Verificación:** No hay entradas con sets vacíos en el índice (se eliminan al quedar vacías).

---

### 1.4 Complejidades Temporales y Espaciales

#### Complejidad Temporal

| Operación                         | Complejidad  | Justificación                         |
| --------------------------------- | ------------ | ------------------------------------- |
| `crear()`                         | O(1)         | Inicialización de estructuras         |
| `loadFromJson(ruta)`              | O(n)         | n = cantidad de clientes en JSON      |
| `agregarCliente(nombre, scoring)` | O(log n)     | HashMap O(1) + TreeMap O(log n)       |
| `buscarPorNombre(nombre)`         | O(1) prom    | HashMap lookup                        |
| `buscarPorScoring(scoring)`       | O(log n + k) | TreeMap lookup + k resultados         |
| `buscarPorScoringEntre(min, max)` | O(log n + m) | TreeMap subMap + m resultados         |
| `undo()` ADD_CLIENT               | O(n)         | Limpieza de referencias en n clientes |
| `undo()` REQUEST_FOLLOW           | O(1)         | Deque removeLast                      |
| `solicitarSeguir(...)`            | O(1)         | Deque addLast                         |
| `procesarSiguienteSolicitud()`    | O(1)         | Deque pollFirst                       |
| `cantidadClientes()`              | O(1)         | Map.size()                            |
| `cantidadSolicitudesPendientes()` | O(1)         | Deque.size()                          |

**Notación:**

- `n` = total de clientes en el sistema
- `k` = cantidad de clientes con un scoring específico
- `m` = cantidad de clientes en un rango de scoring

#### Complejidad Espacial

| Estructura          | Espacio          | Justificación                              |
| ------------------- | ---------------- | ------------------------------------------ |
| `clientesPorNombre` | O(n)             | n clientes                                 |
| `indicePorScoring`  | O(n)             | n nombres distribuidos en sets             |
| `historial`         | O(h)             | h = cantidad de acciones realizadas        |
| `colaSeguimientos`  | O(s)             | s = solicitudes pendientes                 |
| **Total**           | **O(n + h + s)** | Lineal en clientes, acciones y solicitudes |

**Observaciones:**

- En el peor caso, `h` puede crecer indefinidamente (cada acción se registra)
- En el peor caso, `s` puede acumularse sin procesamiento
- El índice `indicePorScoring` tiene espacio proporcional a la cantidad de clientes únicos
- Cliente tiene espacio O(1) + O(f + c) donde:
  - f = cantidad de "siguiendo" (máx 2 en Iteración 2)
  - c = cantidad de conexiones

---

### 1.5 Decisiones de Diseño

#### 1.5.1 HashMap para búsqueda por nombre

**Razón:** O(1) promedio es óptimo para búsquedas frecuentes por clave única.
**Alternativa rechazada:** Lista O(n) o ABB O(log n).

#### 1.5.2 TreeMap para índice por scoring

**Razón:** Permite búsquedas por rango O(log n + m) y mantiene orden automático.
**Alternativa rechazada:** HashMap no soporta rangos eficientemente.

#### 1.5.3 ArrayDeque para historial y cola

**Razón:** O(1) para push/pop (PILA) y addLast/pollFirst (COLA).
**Alternativa rechazada:** ArrayList tiene O(n) para remover del inicio.

#### 1.5.4 Limpieza de referencias en undo ADD_CLIENT

**Razón:** Evita inconsistencias (clientes inexistentes en siguiendo/conexiones).
**Costo:** O(n) pero necesario para mantener IR7.

#### 1.5.5 loadFromJson NO registra en historial

**Razón:** La carga inicial no es una "acción del usuario" que deba deshacerse.
**Validación:** Tests verifican `historial.isEmpty()` después de carga.

#### 1.5.6 Optional para undo

**Razón:** API moderna y explícita (mejor que retornar null o lanzar excepción).
**Uso:** `undo().ifPresent(action -> ...)`

---

### 1.6 Casos de Uso Cubiertos por Tests

#### Tests de JSON:

- ✅ Carga exitosa con formato `{"clientes": [...]}`
- ✅ JSON malformado → IllegalArgumentException
- ✅ Cliente duplicado → IllegalArgumentException
- ✅ Scoring negativo → IllegalArgumentException
- ✅ JSON vacío (`[]`) → comportamiento estable (0 clientes)

#### Tests de Clientes:

- ✅ Agregar cliente válido
- ✅ Agregar cliente con nombre vacío/null → IllegalArgumentException
- ✅ Buscar por nombre existente
- ✅ Buscar por nombre inexistente → null
- ✅ Buscar por scoring exacto
- ✅ Buscar por rango de scoring

#### Tests de Historial/Undo:

- ✅ Registrar acción ADD_CLIENT y deshacer
- ✅ Registrar acción REQUEST_FOLLOW y deshacer
- ✅ Undo sin acciones → Optional.empty()
- ✅ Undo ADD_CLIENT limpia referencias en otros clientes

#### Tests de Cola FIFO:

- ✅ Solicitudes se procesan en orden FIFO
- ✅ Procesar solicitud con cola vacía → null
- ✅ Cantidad de solicitudes pendientes correcta

---

## Iteración 2 - Seguimientos y ABB

### 2.1 Nuevas Estructuras

#### TAD Cliente (actualizado)

```java
rep Cliente {
    String nombre;              // inmutable
    int scoring;                // inmutable
    Set<String> siguiendo;      // máximo 2 elementos (NUEVA RESTRICCIÓN)
    Set<String> conexiones;     // para Iteración 3
    int followersCount;         // contador de seguidores (NUEVO)
}
```

#### TAD NodoABB

```java
rep NodoABB {
    int scoring;                // clave de ordenamiento
    List<Cliente> clientes;     // clientes con mismo scoring
    NodoABB izquierdo;
    NodoABB derecho;
}
```

#### TAD ArbolBinarioBusqueda

```java
rep ArbolBinarioBusqueda {
    NodoABB raiz;
    int size;                   // cantidad total de clientes insertados
}
```

---

### 2.2 Operaciones Nuevas/Actualizadas

#### Cliente - Validaciones de Seguimiento

```
seguirA(String nombreCliente) → void
  Pre: nombreCliente != null
       && nombreCliente != this.nombre (no seguirse a sí mismo)
       && nombreCliente ∉ siguiendo (no duplicar)
       && |siguiendo| < 2 (máximo 2 seguimientos)
  Post: agrega nombreCliente a siguiendo
  Lanza:
    - IllegalArgumentException si intenta seguirse a sí mismo o duplicar
    - IllegalStateException si ya tiene 2 seguimientos
  Complejidad: O(1) promedio (HashSet)

incrementarFollowers() → void
  Post: followersCount++
  Complejidad: O(1)

decrementarFollowers() → void
  Post: followersCount-- (si > 0)
  Complejidad: O(1)

getFollowersCount() → int
  Complejidad: O(1)
```

#### RedSocialEmpresarial - Confirmar Seguimiento

```
confirmarSeguimiento(String solicitante, String objetivo) → void
  Pre: existeCliente(solicitante) && existeCliente(objetivo)
  Post: solicitante.seguirA(objetivo)
        objetivo.incrementarFollowers()
  Lanza: IllegalArgumentException o IllegalStateException según validaciones
  Complejidad: O(1) promedio
```

#### RedSocialEmpresarial - ABB y Nivel 4

```
obtenerClientesNivel4() → List<Cliente>
  Post: retorna clientes del nivel 4 del ABB (0-indexed)
        ordenados por followersCount descendente
  Complejidad: O(n) BFS + O(k log k) sort, k = clientes en nivel 4

getABB() → ArbolBinarioBusqueda
  Post: retorna referencia al ABB interno
  Complejidad: O(1)
```

---

### 2.3 Operaciones ABB

#### Inserción

```
insertar(Cliente c) → void
  Pre: c != null
  Post: inserta cliente en nodo según scoring
        si ya existe nodo con ese scoring, agrega a lista
  Complejidad: O(log n) promedio, O(n) peor caso (desbalanceado)
```

#### Obtener Nivel (BFS)

```
obtenerNivel(int nivel) → List<Cliente>
  Pre: nivel >= 0
  Post: retorna todos los clientes en ese nivel (BFS)
        nivel 0 = raíz, nivel 1 = hijos de raíz, etc.
  Complejidad: O(n) peor caso - recorre hasta ese nivel

obtenerNivel4() → List<Cliente>
  Post: wrapper de obtenerNivel(4)
        ordena resultado por followersCount descendente
  Complejidad: O(n) + O(k log k), k = clientes en nivel 4
```

#### Recorrido Inorder

```
inorder() → List<Cliente>
  Post: retorna todos los clientes ordenados por scoring (menor a mayor)
  Complejidad: O(n)
```

#### Altura

```
altura() → int
  Post: retorna altura del árbol
        raíz = altura 0, vacío = altura -1
  Complejidad: O(n)
```

---

### 2.4 Invariantes Adicionales (Iteración 2)

#### IR9: Máximo 2 seguimientos

```
∀ cliente ∈ clientesPorNombre.values() ⇒ |cliente.getSiguiendo()| <= 2
```

**Verificación:** Todo cliente sigue a lo sumo 2 clientes.

#### IR10: No auto-seguimiento

```
∀ cliente ∈ clientesPorNombre.values() ⇒
    cliente.getNombre() ∉ cliente.getSiguiendo()
```

**Verificación:** Ningún cliente se sigue a sí mismo.

#### IR11: followersCount consistente

```
∀ cliente ∈ clientesPorNombre.values() ⇒
    cliente.getFollowersCount() ==
    |{c ∈ clientesPorNombre.values() | cliente.getNombre() ∈ c.getSiguiendo()}|
```

**Verificación:** El contador de seguidores coincide con la cantidad de clientes que lo siguen.

#### IR12: ABB sincronizado con clientes

```
|abb.inorder()| == |clientesPorNombre|
∀ cliente ∈ clientesPorNombre.values() ⇒ cliente ∈ abb.inorder()
```

**Verificación:** Todo cliente del sistema está en el ABB.

#### IR13: ABB ordenado por scoring

```
∀ nodo ∈ abb ⇒
    (nodo.izquierdo == null ∨ nodo.izquierdo.scoring < nodo.scoring) ∧
    (nodo.derecho == null ∨ nodo.derecho.scoring > nodo.scoring)
```

**Verificación:** El ABB mantiene la propiedad de orden por scoring.

#### IR14: Nodos ABB sin listas vacías

```
∀ nodo ∈ abb ⇒ |nodo.clientes| > 0
```

**Verificación:** Todo nodo tiene al menos un cliente en su lista.

---

### 2.5 Complejidades Actualizadas (Iteración 2)

#### Complejidad Temporal Adicional

| Operación                        | Complejidad    | Justificación                                  |
| -------------------------------- | -------------- | ---------------------------------------------- |
| `Cliente.seguirA(nombre)`        | O(1) prom      | HashSet contains + add                         |
| `Cliente.incrementarFollowers()` | O(1)           | Incremento de contador                         |
| `confirmarSeguimiento(a, b)`     | O(1) prom      | seguirA() + incrementarFollowers()             |
| `abb.insertar(cliente)`          | O(log n) prom  | ABB balanceado esperado                        |
| `abb.obtenerNivel(k)`            | O(n)           | BFS hasta nivel k                              |
| `abb.obtenerNivel4()`            | O(n + k log k) | BFS + sort de k elementos                      |
| `abb.inorder()`                  | O(n)           | Recorrido completo del árbol                   |
| `abb.altura()`                   | O(n)           | Recorrido recursivo                            |
| `loadFromJson` (actualizado)     | O(n log n)     | n inserciones en ABB + actualización followers |

**Notación adicional:**

- `k` = cantidad de clientes en un nivel específico del ABB

#### Complejidad Espacial Adicional

| Estructura               | Espacio  | Justificación                    |
| ------------------------ | -------- | -------------------------------- |
| `Cliente.followersCount` | O(1)     | Un entero por cliente            |
| `ABB` (NodoABB)          | O(n)     | n clientes distribuidos en nodos |
| `NodoABB.clientes`       | O(c)     | c = clientes con mismo scoring   |
| **Total ABB**            | **O(n)** | Lineal en cantidad de clientes   |

**Observaciones:**

- El ABB duplica el espacio de clientes (están en HashMap Y en ABB)
- Espacio total sigue siendo O(n) ya que es una copia de referencias
- En el peor caso (scoring todos distintos): n nodos con 1 cliente cada uno
- En el mejor caso (scoring todos iguales): 1 nodo con n clientes (altura 0)

---

### 2.6 Decisiones de Diseño (Iteración 2)

#### 2.6.1 ABB propio vs TreeMap

**Razón:** El enunciado requiere "ABB visto en clase", no se acepta TreeMap como reemplazo.  
**Beneficio:** Control total sobre estructura y operaciones (obtenerNivel no está en TreeMap).  
**Costo:** Más código, sin auto-balanceo (pero suficiente para el TP).

#### 2.6.2 Nivel 4 para "ver quién tiene más seguidores"

**Interpretación:** El nivel del ABB no tiene relación semántica con seguidores.  
**Solución implementada:** Obtener nivel 4 del ABB y ordenarlo por followersCount descendente.  
**Justificación:** Cumple requisito del enunciado de forma defendible.

#### 2.6.3 Clientes con mismo scoring en lista (no subárboles)

**Razón:** Simplifica inserción y evita criterios artificiales de desempate.  
**Alternativa rechazada:** Desempatar por nombre (más complejo, sin beneficio claro).

#### 2.6.4 followersCount como contador local

**Razón:** O(1) para consultar, evita recorrer todos los clientes cada vez.  
**Costo:** Debe mantenerse sincronizado (IR11).  
**Actualización:** Se incrementa en confirmarSeguimiento() y en loadFromJson().

#### 2.6.5 Validaciones en Cliente.seguirA()

**Razón:** Encapsulación de lógica de negocio en el modelo.  
**Consistencia:** RedSocialEmpresarial invoca seguirA() y confía en sus validaciones.  
**Excepciones:**

- `IllegalArgumentException`: auto-seguimiento, duplicado
- `IllegalStateException`: máximo 2 alcanzado

#### 2.6.6 Rechazo estricto en loadFromJson

**Razón:** Mantener integridad desde la carga inicial.  
**Política:** Si JSON tiene cliente con >2 seguimientos → IllegalArgumentException.  
**Alternativa rechazada:** Truncar a 2 con warning (menos predecible).

---

### 2.7 Casos de Uso Cubiertos por Tests (Iteración 2)

#### Tests de Seguimientos:

- ✅ Máximo 2 seguimientos (tercer intento falla)
- ✅ No seguirse a sí mismo → IllegalArgumentException
- ✅ No duplicar seguimiento → IllegalArgumentException
- ✅ confirmarSeguimiento incrementa followersCount
- ✅ loadFromJson rechaza cliente con >2 seguimientos
- ✅ loadFromJson actualiza followersCount correctamente

#### Tests de ABB:

- ✅ Insertar clientes y verificar orden inorder (por scoring)
- ✅ ABB no vacío después de inserciones
- ✅ ABB size correcto
- ✅ obtenerNivel4 retorna clientes del nivel 4
- ✅ obtenerNivel4 ordena por followersCount descendente

#### Total Tests Iteración 1 + 2:

- **20 tests** de Iteración 1
- **9 tests** de Iteración 2
- **Total: 29 tests** ✅ todos pasando

---

## Iteración 3 - Grafo y Distancias

### 3.1 Nueva Estructura

#### TAD GrafoConexiones

```java
rep GrafoConexiones {
    Map<String, Set<String>> adyacencias;  // nombre -> conjunto de vecinos
}
```

**Característica:** Grafo no dirigido (bidireccional), no ponderado.

**Invariante de simetría:** Si cliente1 está en adyacencias[cliente2], entonces cliente2 está en adyacencias[cliente1].

---

### 3.2 Operaciones del Grafo

#### Agregar Conexión

```
agregarConexion(String cliente1, String cliente2) → void
  Pre: cliente1 != null && cliente2 != null
       && cliente1 != cliente2 (no auto-loops)
  Post: agrega arista bidireccional entre cliente1 y cliente2
        si ya existe, no hace nada (idempotente)
  Complejidad: O(1) promedio (HashMap + HashSet)
```

#### Obtener Vecinos

```
vecinos(String cliente) → Set<String>
  Pre: cliente != null
  Post: retorna conjunto inmutable de vecinos directos
        si el cliente no tiene conexiones → conjunto vacío
  Complejidad: O(1) promedio
```

#### Agregar Cliente

```
agregarCliente(String cliente) → void
  Pre: cliente != null
  Post: agrega cliente sin conexiones al grafo
        si ya existe, no hace nada
  Complejidad: O(1)
```

#### Calcular Distancia (BFS)

```
calcularDistancia(String origen, String destino) → int
  Pre: origen != null && destino != null
       && origen ∈ grafo && destino ∈ grafo
  Post: retorna distancia en saltos (aristas) entre origen y destino
        0 si origen == destino
        -1 si no hay camino
  Lanza: IllegalArgumentException si origen o destino no existen en grafo
  Complejidad: O(V + E) siendo V = vértices, E = aristas
  Implementación: BFS estándar con Queue y Set de visitados
```

#### Operaciones Auxiliares

```
existeConexion(cliente1, cliente2) → boolean
  Complejidad: O(1) promedio

cantidadClientes() → int
  Complejidad: O(1)

cantidadConexiones() → int
  Complejidad: O(V) - suma grados / 2

estaVacio() → boolean
  Complejidad: O(1)

clientes() → Set<String>
  Complejidad: O(1)
```

---

### 3.3 Integración en RedSocialEmpresarial

#### Representación Actualizada

```java
rep RedSocialEmpresarial {
    // ... (estructuras anteriores)
    GrafoConexiones grafo;  // NUEVA: centraliza conexiones
}
```

#### Operaciones Nuevas

```
obtenerVecinos(String nombreCliente) → Set<String>
  Post: delega en grafo.vecinos(nombreCliente)
  Complejidad: O(1) promedio

calcularDistancia(String origen, String destino) → int
  Post: delega en grafo.calcularDistancia(origen, destino)
  Complejidad: O(V + E)

agregarConexion(String cliente1, String cliente2) → void
  Pre: existeCliente(cliente1) && existeCliente(cliente2)
  Post: agrega conexión en grafo
        sincroniza con Cliente.agregarConexion() (bidireccional)
  Complejidad: O(1) promedio

existeConexion(String cliente1, String cliente2) → boolean
  Complejidad: O(1) promedio

getGrafo() → GrafoConexiones
  Complejidad: O(1)
```

#### Actualización de loadFromJson (Iteración 3)

```
loadFromJson(ruta):
  1. Cargar clientes (primera pasada)
  2. Actualizar followersCount (segunda pasada)
  3. Cargar conexiones en grafo (tercera pasada):
     - Por cada cliente c:
       - Por cada conexion ∈ c.getConexiones():
         - si conexion no existe → warning y continuar
         - sino grafo.agregarConexion(c.nombre, conexion)
```

**Política de conexiones inválidas:** Se ignoran con warning en stderr, no se detiene la carga.

---

### 3.4 Invariantes Adicionales (Iteración 3)

#### IR15: Grafo sincronizado con clientes

```
∀ nombre ∈ grafo.clientes() ⇒ nombre ∈ clientesPorNombre.keys()
```

**Verificación:** Todo cliente del grafo existe en el sistema.

#### IR16: Simetría de conexiones en grafo

```
∀ u, v ∈ grafo.clientes() ⇒
    (u ∈ grafo.vecinos(v)) ⟺ (v ∈ grafo.vecinos(u))
```

**Verificación:** Las conexiones son bidireccionales (grafo no dirigido).

#### IR17: No auto-loops

```
∀ u ∈ grafo.clientes() ⇒ u ∉ grafo.vecinos(u)
```

**Verificación:** Ningún cliente está conectado consigo mismo.

#### IR18: Consistencia Cliente.conexiones con grafo (deseable)

```
∀ cliente ∈ clientesPorNombre.values() ⇒
    cliente.getConexiones() ⊆ grafo.vecinos(cliente.getNombre())
```

**Verificación:** Las conexiones del modelo Cliente están reflejadas en el grafo.

**Nota:** Este invariante puede relajarse si se decide usar solo el grafo como fuente de verdad.

---

### 3.5 Complejidades Actualizadas (Iteración 3)

#### Complejidad Temporal Adicional

| Operación                       | Complejidad    | Justificación                         |
| ------------------------------- | -------------- | ------------------------------------- |
| `grafo.agregarConexion(u, v)`   | O(1) prom      | HashMap.computeIfAbsent + HashSet.add |
| `grafo.vecinos(u)`              | O(1) prom      | HashMap.get                           |
| `grafo.calcularDistancia(u, v)` | O(V + E)       | BFS: Queue + visitados + recorrido    |
| `grafo.existeConexion(u, v)`    | O(1) prom      | HashSet.contains                      |
| `grafo.cantidadClientes()`      | O(1)           | Map.size()                            |
| `grafo.cantidadConexiones()`    | O(V)           | Suma de grados / 2                    |
| `loadFromJson` (Iteración 3)    | O(n log n + m) | n clientes, m conexiones              |

**Notación adicional:**

- `V` = cantidad de vértices (clientes en grafo)
- `E` = cantidad de aristas (conexiones bidireccionales)
- En grafo conectado: E ≤ V(V-1)/2 (máximo grafo completo)

#### Complejidad Espacial Adicional

| Estructura        | Espacio                  | Justificación                                                |
| ----------------- | ------------------------ | ------------------------------------------------------------ |
| `GrafoConexiones` | O(V + E)                 | V claves + E aristas (cada una x2 por bidireccional)         |
| `BFS (temporal)`  | O(V)                     | Queue + visitados + distancias                               |
| **Total sistema** | **O(n + h + s + V + E)** | n clientes, h acciones, s solicitudes, V vértices, E aristas |

**Observaciones:**

- Como V = n (todos los clientes están en el grafo), el espacio es O(n + E)
- En el peor caso: E = O(n²) (grafo completo)
- En el caso típico: E = O(n) (grafo disperso)

---

### 3.6 Decisiones de Diseño (Iteración 3)

#### 3.6.1 Grafo como estructura separada

**Razón:** Centralizar lógica de conexiones y algoritmos de grafos (BFS, DFS futuros).  
**Beneficio:** Separación de responsabilidades, fácil testing.  
**Costo:** Duplicación potencial con Cliente.conexiones (mitigado con sincronización).

#### 3.6.2 BFS para distancia (no DFS)

**Razón:** BFS garantiza camino más corto en grafo no ponderado.  
**Alternativa rechazada:** DFS no garantiza camino mínimo.  
**Complejidad:** O(V + E) en ambos casos, pero BFS es óptimo para distancia.

#### 3.6.3 Grafo bidireccional (no dirigido)

**Razón:** Las "conexiones" en el contexto empresarial son simétricas (contactos mutuos).  
**Implementación:** agregarConexion() agrega en ambas direcciones.  
**Validación:** No confundir con "seguimientos" que SÍ son dirigidos (unidireccionales).

#### 3.6.4 Política de conexiones inválidas en JSON

**Razón:** Tolerancia a errores en carga inicial.  
**Política adoptada:** Ignorar con warning en stderr, continuar carga.  
**Alternativa rechazada:** Lanzar excepción (demasiado estricto para datos reales).

#### 3.6.5 Sincronización Cliente.conexiones con grafo

**Razón:** Mantener consistencia con modelo existente.  
**Método:** agregarConexion() actualiza ambos (Cliente y grafo).  
**Mejora futura:** Eliminar Cliente.conexiones y usar solo grafo (fuente única de verdad).

#### 3.6.6 BFS retorna -1 si no hay camino

**Razón:** Convención estándar en algoritmos de grafos.  
**Alternativa rechazada:** Lanzar excepción (menos conveniente para verificar conexidad).  
**Caso especial:** origen == destino → distancia 0 (sin necesidad de BFS).

---

### 3.7 Casos de Uso Cubiertos por Tests (Iteración 3)

#### Tests de Grafo y Vecinos:

- ✅ Agregar conexión bidireccional
- ✅ Vecinos de un cliente
- ✅ Cliente aislado (sin conexiones)
- ✅ Intentar conectar con cliente inexistente → IllegalArgumentException

#### Tests de Distancia BFS:

- ✅ Distancia 0 (mismo cliente)
- ✅ Distancia 1 (vecinos directos)
- ✅ Distancia 2 o más (caminos más largos)
- ✅ Sin camino → devuelve -1
- ✅ Grafo con ciclos (BFS encuentra camino mínimo)

#### Tests de Carga JSON:

- ✅ loadFromJson carga conexiones en el grafo
- ✅ Conexión a cliente inexistente se ignora con warning
- ✅ Verificar vecinos después de carga
- ✅ Distancias correctas después de carga

#### Total Tests Iteración 1 + 2 + 3:

- **20 tests** de Iteración 1
- **9 tests** de Iteración 2
- **11 tests** de Iteración 3
- **Total: 40 tests** ✅ todos pasando

---

## Notas Finales

### Coverage de Tests (Iteración 1)

- **Total de tests:** 20
- **Cobertura:** ~90% de las operaciones públicas de Iteración 1
- **Casos críticos cubiertos:**
  - Límites (vacío, null, scoring 0)
  - Duplicados
  - Consistencia de índices
  - FIFO y LIFO correcto

### Referencias Bibliográficas

- Cormen, T. et al. (2009). _Introduction to Algorithms_ (3rd ed.). MIT Press.
  - Capítulo 12: Binary Search Trees
  - Capítulo 22: Elementary Graph Algorithms (BFS)
- Sedgewick, R. & Wayne, K. (2011). _Algorithms_ (4th ed.). Addison-Wesley.
  - Capítulo 3: Searching (Hash Tables, BST, Red-Black BST)

---

**Última actualización:** 23 de febrero de 2026  
**Versión:** 1.0 - Iteración 1 completa
