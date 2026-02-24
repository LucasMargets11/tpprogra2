# Tests Manuales - Red Social Empresarial

**Documento de pruebas manuales para validación funcional del TP**  
**Iteración 1/2/3 - AyED II - UADE**

---

## Índice

1. [Cómo Ejecutar el Runner](#cómo-ejecutar-el-runner)
2. [Casos de Prueba](#casos-de-prueba)
3. [Resultados Esperados](#resultados-esperados)

---

## Cómo Ejecutar el Runner

### Opción 1: Con Maven Exec Plugin (recomendado)

```bash
mvn exec:java
```

Con JSON automático:

```bash
mvn exec:java -Dexec.args="demo.json"
```

### Opción 2: Compilar y ejecutar con classpath completo

```bash
# Compilar y copiar dependencias
mvn clean compile dependency:copy-dependencies

# Ejecutar
java -cp "target/classes;target/dependency/*" ar.uade.redsocial.DemoApp
```

### Opción 3: Crear JAR ejecutable con dependencias

```bash
# Compilar JAR con dependencias incluidas
mvn clean package

# Ejecutar
java -jar target/red-social-empresarial-1.0-SNAPSHOT.jar
```

---

## Casos de Prueba

### CASO A: Carga de JSON y Snapshot

**Objetivo:** Verificar que la carga de datos desde JSON funciona correctamente.

**Pasos:**

1. Iniciar el runner: `mvn -q exec:java -Dexec.mainClass=ar.uade.redsocial.DemoApp`
2. Seleccionar opción `1` (Load demo JSON)
3. Ingresar ruta: `demo.json` (o presionar Enter para usar default)
4. Seleccionar opción `2` (GET Snapshot)

**Resultado Esperado:**

- ✅ JSON cargado sin errores
- ✅ `cantidadClientes`: 8 (según demo.json)
- ✅ Snapshot muestra estructura completa con:
  - Lista de clientes con scoring, followersCount, siguiendo, conexiones
  - Índice por scoring (TreeMap)
  - Solicitudes pendientes (inicial: 0)
  - Información del ABB y grafo

**Evidencia:**

```json
{
  "cantidadClientes": 8,
  "cantidadSolicitudesPendientes": 0,
  "alturaABB": 3,
  "clientesEnGrafo": 8,
  "conexionesEnGrafo": 4,
  "clientes": [
    {
      "nombre": "Ana",
      "scoring": 95,
      "followersCount": 3,
      "siguiendo": ["Bruno", "Diego"],
      "conexiones": ["Carla"]
    },
    ...
  ]
}
```

---

### CASO B: Validación Máximo 2 Seguimientos

**Objetivo:** Verificar que un cliente no puede seguir a más de 2 clientes.

**Pasos:**

1. Cargar `demo.json` (opción `1`)
2. Seleccionar opción `3` (GET Clientes) para ver quién sigue a quién
3. Identificar un cliente con 2 seguimientos (ej: Ana sigue a Bruno y Diego)
4. Intentar agregar un 3er seguimiento:
   - Seleccionar opción `10` (Solicitar seguir)
   - Solicitante: `Ana`
   - Objetivo: `Carla` (un cliente que Ana NO sigue)
5. Seleccionar opción `11` (Procesar solicitud)

**Resultado Esperado:**

- ✅ La solicitud se encola correctamente (opción 10)
- ❌ Al procesar (opción 11), falla con error: **"No se puede seguir a más de 2 clientes"**
- ✅ Mensaje: `⚠️ Solicitud procesada pero falló confirmación: IllegalStateException`

**Variante - Crear cliente nuevo y probar límite:**

1. Opción `9` (Crear cliente): nombre `Test1`, scoring `50`
2. Opción `9` (Crear cliente): nombre `Test2`, scoring `60`
3. Opción `9` (Crear cliente): nombre `Test3`, scoring `70`
4. Opción `9` (Crear cliente): nombre `Test4`, scoring `80`
5. Solicitar seguir (opción `10`): `Test1` -> `Test2` (OK)
6. Solicitar seguir (opción `10`): `Test1` -> `Test3` (OK)
7. Solicitar seguir (opción `10`): `Test1` -> `Test4` (debe fallar al procesar)
8. Procesar 3 solicitudes (opción `11` x3)

**Resultado:**

- ✅ Primeras 2 confirmadas
- ❌ Tercera rechazada por máximo alcanzado

---

### CASO C: Conexiones y Distancia (BFS)

**Objetivo:** Verificar que las conexiones bidireccionales funcionan y el BFS calcula distancias correctamente.

**Pasos:**

1. Cargar `demo.json` (opción `1`)
2. Verificar conexiones existentes:
   - Opción `6` (GET Conexiones/Vecinos)
   - Ingresar: `Ana` → debe mostrar `Carla` como vecino
   - Ingresar: `Carla` → debe mostrar `Ana`, `Bruno`, `Hugo`
3. Agregar nueva conexión:
   - Opción `12` (Agregar conexión)
   - Cliente 1: `Elena`
   - Cliente 2: `Facundo`
4. Verificar conexión:
   - Opción `6` (GET Conexiones/Vecinos)
   - Ingresar: `Elena` → debe incluir `Facundo`
5. Calcular distancias:
   - Opción `8` (Calcular distancia)
   - Caso 1: Origen `Ana`, Destino `Ana` → Distancia: **0** (mismo cliente)
   - Caso 2: Origen `Ana`, Destino `Carla` → Distancia: **1** (vecinos directos)
   - Caso 3: Origen `Ana`, Destino `Hugo` → Distancia: **2** (Ana-Carla-Hugo)
   - Caso 4: Origen `Ana`, Destino `Facundo` → Distancia: **-1** (sin camino, componente desconectada)

**Resultado Esperado:**

- ✅ Todas las distancias calculadas correctamente con BFS
- ✅ Conexiones bidireccionales (A-B implica B-A)
- ✅ Componentes desconectadas retornan -1

**Grafo de conexiones en demo.json:**

```
Ana ─── Carla ─── Bruno
         │
         └─── Hugo

Elena ─── Bruno

(Después de agregar Elena-Facundo)
Elena ─── Facundo
```

---

### CASO D: ABB Nivel 4

**Objetivo:** Verificar que el ABB se construye correctamente y el nivel 4 se calcula con BFS.

**Pasos:**

1. Cargar `demo.json` (opción `1`)
2. Seleccionar opción `7` (ABB Nivel 4)

**Resultado Esperado:**

- ⚠️ Posiblemente mensaje: **"No hay clientes en el nivel 4 del ABB"**
- **Razón:** Con 8 clientes de demo.json, el ABB puede tener menos de 5 niveles (0-4)
- **Interpretación:** Nivel calculado depende del orden de inserción por scoring

**Para probar con árbol más profundo:**

1. Cargar JSON con más clientes (crear `demo_extended.json` con 20+ clientes)
2. O crear clientes manualmente (opción `9`) con scorings intermedios:
   - Ej: 50, 25, 75, 10, 40, 60, 90, 5, 15, 30, 45, 55, 65, 85, 95, 3, 7
3. Seleccionar opción `7` (ABB Nivel 4)
4. Verificar que muestra clientes ordenados por `followersCount` descendente

**Nivel del ABB:**

- **Nivel 0:** Raíz (primer cliente insertado por scoring)
- **Nivel 1:** Hijos de raíz (scoring < raíz e > raíz)
- **Nivel 2:** Nietos
- **Nivel 3:** Bisnietos
- **Nivel 4:** Tataranietos

**Evidencia:**

```
Total: 2 cliente(s) en nivel 4

Nombre               |  Scoring |  Followers
---------------------------------------------
ClienteA             |        3 |          2
ClienteB             |        7 |          1
```

---

### CASO E: Undo Revierte Efectos

**Objetivo:** Verificar que undo revierte efectos reales (no solo pop del historial).

**Pasos:**

#### E1: Undo de Crear Cliente

1. Opción `2` (GET Snapshot) → anotar `cantidadClientes` inicial (ej: 8)
2. Opción `9` (Crear cliente): nombre `UndoTest`, scoring `99`
3. Opción `2` (GET Snapshot) → verificar `cantidadClientes` = 9
4. Opción `13` (Undo)
5. Opción `2` (GET Snapshot) → verificar `cantidadClientes` = 8
6. Opción `3` (GET Clientes) → verificar que `UndoTest` NO aparece

**Resultado Esperado:**

- ✅ Cliente creado temporalmente
- ✅ Undo lo elimina del sistema
- ✅ Cliente ya no existe en HashMap, TreeMap, ABB

#### E2: Undo de Solicitar Seguir

1. Opción `10` (Solicitar seguir): `Ana` -> `Carla`
2. Verificar: `cantidadSolicitudesPendientes` = 1
3. Opción `13` (Undo)
4. Verificar: `cantidadSolicitudesPendientes` = 0

**Resultado Esperado:**

- ✅ Solicitud eliminada de la cola FIFO
- ✅ Undo revierte `addLast()` con `removeLast()`

#### E3: Undo Limpia Referencias

**Escenario complejo:**

1. Crear cliente `ClienteA` (opción `9`)
2. Crear cliente `ClienteB` (opción `9`)
3. Solicitar seguir: `ClienteB` -> `ClienteA` (opción `10`)
4. Procesar solicitud (opción `11`)
5. Verificar que `ClienteB` sigue a `ClienteA` (opción `5`)
6. Undo (opción `13`) → deshace solicitud
7. Undo (opción `13`) → deshace creación de `ClienteB`
8. Undo (opción `13`) → deshace creación de `ClienteA`
9. Opción `3` (GET Clientes) → verificar que ambos NO existen

**Resultado Esperado:**

- ✅ Undo de ADD_CLIENT limpia referencias en otros clientes
- ✅ Si `ClienteB` seguía a `ClienteA`, al deshacer `ClienteA`, `ClienteB` deja de seguirlo
- ✅ Consistencia mantenida en todas las estructuras

---

## Resultados Esperados Generales

### ✅ Funcionalidades Que Deben Funcionar

| #   | Funcionalidad          | Validación                                    |
| --- | ---------------------- | --------------------------------------------- |
| 1   | Carga JSON             | Sin errores, `cantidadClientes` correcta      |
| 2   | Snapshot               | JSON completo con todas las estructuras       |
| 3   | GET Clientes           | Lista ordenada por scoring con followersCount |
| 4   | GET Scoring Index      | TreeMap con scorings y listas de nombres      |
| 5   | GET Siguiendo          | Set de clientes que un cliente sigue          |
| 6   | GET Vecinos            | Set de conexiones del grafo                   |
| 7   | ABB Nivel 4            | Lista de clientes en nivel 4 (si existe)      |
| 8   | Calcular Distancia BFS | Distancias correctas (0, positivo, -1)        |
| 9   | Crear Cliente          | Cliente agregado a todas las estructuras      |
| 10  | Solicitar Seguir       | Solicitud encolada en FIFO                    |
| 11  | Procesar Solicitud     | Dequeue FIFO + confirmar con validaciones     |
| 12  | Agregar Conexión       | Conexión bidireccional en grafo               |
| 13  | Undo                   | Revierte efectos reales en estructuras        |

### ❌ Errores Esperados (Validaciones)

| Caso                     | Error Esperado             | Mensaje                                   |
| ------------------------ | -------------------------- | ----------------------------------------- |
| Seguir a más de 2        | `IllegalStateException`    | "No se puede seguir a más de 2 clientes"  |
| Seguirse a sí mismo      | `IllegalArgumentException` | "Un cliente no puede seguirse a sí mismo" |
| Duplicar seguimiento     | `IllegalArgumentException` | "Ya está siguiendo a..."                  |
| Cliente inexistente      | `IllegalArgumentException` | "Cliente no encontrado"                   |
| Conexión a mismo cliente | `IllegalArgumentException` | "No se permiten auto-conexiones"          |
| Scoring negativo         | `IllegalArgumentException` | "Scoring inválido"                        |

---

## Checklist de Validación

Marcar cada caso ejecutado:

- [ ] **CASO A:** Carga JSON + Snapshot ✅
- [ ] **CASO B:** Máximo 2 seguimientos ❌ (falla esperado)
- [ ] **CASO C:** Conexiones + BFS distancias ✅
- [ ] **CASO D:** ABB Nivel 4 ✅ (o mensaje esperado)
- [ ] **CASO E1:** Undo crear cliente ✅
- [ ] **CASO E2:** Undo solicitud seguir ✅
- [ ] **CASO E3:** Undo limpia referencias ✅

**Criterio de Aprobación:** 6/7 casos pasan (CASO B falla esperado cuenta como ✅)

---

## Datos de Test (demo.json)

El archivo `demo.json` contiene:

```json
{
  "clientes": [
    {
      "nombre": "Ana",
      "scoring": 95,
      "siguiendo": ["Bruno", "Diego"],
      "conexiones": ["Carla"]
    },
    {
      "nombre": "Bruno",
      "scoring": 82,
      "siguiendo": ["Ana", "Elena"],
      "conexiones": ["Carla"]
    },
    {
      "nombre": "Carla",
      "scoring": 67,
      "siguiendo": ["Ana"],
      "conexiones": ["Ana", "Bruno", "Hugo"]
    },
    {
      "nombre": "Diego",
      "scoring": 40,
      "siguiendo": ["Ana"],
      "conexiones": []
    },
    {
      "nombre": "Elena",
      "scoring": 15,
      "siguiendo": [],
      "conexiones": ["Bruno"]
    },
    {
      "nombre": "Facundo",
      "scoring": 5,
      "siguiendo": ["Elena"],
      "conexiones": []
    },
    {
      "nombre": "Gisela",
      "scoring": 73,
      "siguiendo": ["Ana", "Bruno"],
      "conexiones": []
    },
    {
      "nombre": "Hugo",
      "scoring": 28,
      "siguiendo": ["Gisela"],
      "conexiones": ["Carla"]
    }
  ]
}
```

**Características:**

- 8 clientes
- Scorings: 5, 15, 28, 40, 67, 73, 82, 95
- Conexiones (grafo): Ana-Carla, Bruno-Carla, Hugo-Carla, Elena-Bruno
- Seguimientos: Varios clientes con 1 o 2 seguimientos
- Ana tiene máximo (2 seguimientos): Bruno, Diego

---

## Troubleshooting

### Error: "Cliente origen no existe en el grafo"

**Causa:** El cliente no tiene conexiones cargadas en el grafo.  
**Solución:** Agregar conexión primero (opción 12) o verificar JSON.

### No aparece nivel 4 del ABB

**Causa:** El árbol tiene menos de 5 niveles (altura < 4).  
**Solución:** Agregar más clientes con scorings intermedios para aumentar altura.

### Undo no elimina referencias

**Causa:** Bug en `eliminarClienteCompleto()`.  
**Validación:** Revisar test `testUndo_AddClient_CleanReferences`.

---

**Última actualización:** 24 de febrero de 2026  
**Versión:** 1.0 - Runner interactivo completo
