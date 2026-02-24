# 🚀 RUNNER DE TESTS MANUALES - INSTALACIÓN COMPLETADA

## ✅ Archivos Modificados/Creados

### 1. **src/main/java/ar/uade/redsocial/DemoApp.java**

- ✅ Refactorizado completamente
- ✅ Menú interactivo CLI con 14 opciones
- ✅ Operaciones GET y POST
- ✅ Integración completa con todas las iteraciones (1/2/3)

### 2. **src/main/java/ar/uade/redsocial/service/RedSocialEmpresarial.java**

- ✅ Método `getSnapshot()` agregado
- ✅ Retorna Map completo con estado del sistema
- ✅ Incluye: clientes, índice scoring, ABB, grafo, solicitudes, historial

### 3. **docs/MANUAL_TESTS.md** (NUEVO)

- ✅ 5 casos de prueba reproductibles (A, B, C, D, E)
- ✅ Resultados esperados por cada caso
- ✅ Checklist de validación
- ✅ Troubleshooting

### 4. **pom.xml**

- ✅ `exec-maven-plugin` agregado (versión 3.1.0)
- ✅ Configurado con mainClass: `ar.uade.redsocial.DemoApp`

### 5. **README.md**

- ✅ Sección "Runner de tests manuales" agregada
- ✅ Instrucciones de ejecución
- ✅ Link a casos de prueba

---

## 🎯 Cómo Usar el Runner

### Inicio Rápido

```bash
# Compilar (solo primera vez o si hay cambios)
mvn compile

# Ejecutar runner interactivo
mvn exec:java
```

### Carga Automática de JSON

```bash
mvn exec:java -Dexec.args="demo.json"
```

### Alternativa: Con Classpath Manual

```bash
# Copiar dependencias (solo primera vez)
mvn dependency:copy-dependencies

# Ejecutar
java -cp "target/classes;target/dependency/*" ar.uade.redsocial.DemoApp
```

---

## 📋 Menú del Runner

```
╔════════════════════ MENÚ PRINCIPAL ════════════════════╗
║ [DATOS]                                                ║
║  1. 📂 Load demo JSON                                  ║
║  2. 📸 GET Snapshot (JSON pretty)                      ║
║                                                        ║
║ [CONSULTAS - GET]                                      ║
║  3. 👥 GET Clientes                                    ║
║  4. 📊 GET Scoring Index (TreeMap)                     ║
║  5. 👉 GET Siguiendo (de un cliente)                   ║
║  6. 🔗 GET Conexiones/Vecinos (grafo)                  ║
║  7. 🌲 ABB Nivel 4 (+ followersCount)                  ║
║  8. 📏 Calcular distancia (BFS)                        ║
║                                                        ║
║ [OPERACIONES - POST/PUT]                               ║
║  9. ➕ Crear cliente                                   ║
║ 10. 💌 Solicitar seguir (enqueue)                      ║
║ 11. ⚙️  Procesar solicitud (dequeue + confirmar)       ║
║ 12. 🔗 Agregar conexión (bidireccional)                ║
║ 13. ↩️  Undo (deshacer última acción)                  ║
║ 14. 📜 Historial de acciones                           ║
║                                                        ║
║  0. 🚪 Salir                                           ║
╚════════════════════════════════════════════════════════╝
```

---

## 🧪 Ejemplo de Sesión Manual

### Sesión Típica de Validación:

```bash
# 1. Ejecutar runner
mvn exec:java

# 2. Cargar datos
Seleccione opción: 1
Ruta del archivo JSON [demo.json]: [Enter]
✅ Datos cargados exitosamente desde: D:\Usuario\Desktop\TPPROGRA2\demo.json
   Clientes totales: 8

# 3. Ver snapshot completo
Seleccione opción: 2
📸 SNAPSHOT DEL SISTEMA

{
  "cantidadClientes": 8,
  "cantidadSolicitudesPendientes": 0,
  "alturaABB": 3,
  "clientesEnGrafo": 8,
  "conexionesEnGrafo": 4,
  "clientes": [ ... ],
  ...
}

# 4. Listar clientes
Seleccione opción: 3
👥 LISTA DE CLIENTES

Total: 8 clientes

Nombre               |  Scoring |  Followers |        Siguiendo
----------------------------------------------------------------------
Ana                  |       95 |          3 | [Bruno, Diego]
Bruno                |       82 |          2 | [Ana, Elena]
Gisela               |       73 |          1 | [Ana, Bruno]
...

# 5. Probar seguimiento máximo 2
Seleccione opción: 10
💌 SOLICITAR SEGUIR (enqueue)

Solicitante: Ana
Objetivo: Carla
✅ Solicitud encolada: Ana -> Carla
   Solicitudes pendientes: 1

Seleccione opción: 11
⚙️ PROCESAR SOLICITUD (dequeue + confirmar)

📤 Procesada: Ana -> Carla
⚠️ Solicitud procesada pero falló confirmación: No se puede seguir a más de 2 clientes
   [ERROR ESPERADO - ANA YA SIGUE A 2]

# 6. Calcular distancia BFS
Seleccione opción: 8
📏 CALCULAR DISTANCIA (BFS)

Cliente origen: Ana
Cliente destino: Hugo
✅ Distancia entre 'Ana' y 'Hugo': 2 saltos

# 7. Agregar conexión y recalcular
Seleccione opción: 12
🔗 AGREGAR CONEXIÓN (bidireccional)

Cliente 1: Ana
Cliente 2: Hugo
✅ Conexión agregada: Ana ↔ Hugo

Seleccione opción: 8
📏 CALCULAR DISTANCIA (BFS)

Cliente origen: Ana
Cliente destino: Hugo
✅ Distancia entre 'Ana' y 'Hugo': 1 saltos
   [DISTANCIA REDUCIDA DE 2 A 1]

# 8. Undo
Seleccione opción: 13
↩️ UNDO (deshacer última acción)

✅ Acción deshecha:
   Tipo: REQUEST_FOLLOW
   Detalle: Ana -> Carla
   Fecha/Hora: 2026-02-24T14:30:45.123

# 9. Salir
Seleccione opción: 0
👋 Saliendo... Adiós!
```

---

## ✅ Validación Post-Instalación

### Tests Automatizados (no afectados)

```bash
mvn clean test
```

**Resultado esperado:**

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Tests Manuales (nuevos)

**Ver casos completos en:** [`docs/MANUAL_TESTS.md`](docs/MANUAL_TESTS.md)

**Resumen:**

- ✅ **CASO A:** Carga JSON + Snapshot
- ❌ **CASO B:** Máximo 2 seguimientos (falla esperado)
- ✅ **CASO C:** Conexiones + BFS
- ✅ **CASO D:** ABB Nivel 4
- ✅ **CASO E:** Undo revierte efectos

---

## 📦 Funcionalidades del Runner

### 🔍 Consultas (GET)

| Opción | Funcionalidad            | Operación en TAD                           |
| ------ | ------------------------ | ------------------------------------------ |
| 2      | Snapshot completo        | `getSnapshot()` → JSON pretty              |
| 3      | Lista de clientes        | `buscarPorScoringEntre(MIN, MAX)`          |
| 4      | Índice por scoring       | Acceso a `indicePorScoring`                |
| 5      | A quién sigue un cliente | `Cliente.getSiguiendo()`                   |
| 6      | Vecinos en grafo         | `grafo.vecinos(cliente)`                   |
| 7      | Clientes en nivel 4 ABB  | `abb.obtenerNivel4()`                      |
| 8      | Distancia BFS            | `grafo.calcularDistancia(origen, destino)` |

### ✏️ Operaciones (POST/PUT)

| Opción | Funcionalidad                                 | Operación en TAD                                          |
| ------ | --------------------------------------------- | --------------------------------------------------------- |
| 9      | Crear cliente                                 | `agregarCliente(nombre, scoring)`                         |
| 10     | Solicitar seguir (FIFO enqueue)               | `solicitarSeguir(solicitante, objetivo)`                  |
| 11     | Procesar solicitud (FIFO dequeue + confirmar) | `procesarSiguienteSolicitud()` + `confirmarSeguimiento()` |
| 12     | Agregar conexión bidireccional                | `agregarConexion(cliente1, cliente2)`                     |
| 13     | Undo (reversión de efectos)                   | `undo()`                                                  |

---

## 📁 Estructura Final del Proyecto

```
TPPROGRA2/
├── pom.xml                        ✅ exec-maven-plugin agregado
├── demo.json                      ✅ Datos de prueba
├── README.md                      ✅ Actualizado con runner
├── AUDITORIA_TECNICA.md           ✅ Auditoría completa
├── docs/
│   ├── ANALISIS.md                ✅ Análisis técnico
│   ├── TAD_IR.md                  ✅ TAD e invariantes
│   ├── STATUS.md                  ✅ Estado del proyecto
│   └── MANUAL_TESTS.md            ✅ NUEVO - Casos de prueba manuales
├── src/main/java/ar/uade/redsocial/
│   ├── DemoApp.java               ✅ REFACTORIZADO - Runner interactivo
│   ├── model/
│   │   ├── Cliente.java
│   │   ├── Action.java
│   │   ├── ActionType.java
│   │   └── FollowRequest.java
│   ├── service/
│   │   └── RedSocialEmpresarial.java  ✅ getSnapshot() agregado
│   ├── estructuras/
│   │   ├── ArbolBinarioBusqueda.java
│   │   ├── NodoABB.java
│   │   └── GrafoConexiones.java
│   └── dto/
│       ├── ClienteDTO.java
│       └── RedDTO.java
└── src/test/java/ar/uade/redsocial/
    └── RedSocialEmpresarialTest.java  ✅ 39 tests (no afectados)
```

---

## 🎓 Casos de Uso del Runner

### Para Estudiantes (Defensa)

1. **Demostrar funcionalidad completa:**
   - Cargar JSON → Snapshot → Navegar estructuras
2. **Probar restricciones:**
   - Máximo 2 seguimientos
   - Validaciones del TAD
3. **Algoritmos en acción:**
   - BFS para distancias
   - ABB nivel 4 con BFS
   - FIFO de solicitudes
   - Undo con reversión

### Para Docentes (Evaluación)

1. **Verificar implementaciones:**
   - ABB propio (no TreeMap)
   - Grafo con BFS
   - Undo revierte efectos
2. **Explorar casos borde:**
   - JSON inválido
   - Clientes inexistentes
   - Grafos desconectados
3. **Validar complejidades:**
   - Búsquedas O(1) por nombre
   - Búsquedas O(log n) por scoring
   - BFS O(V+E) para distancias

---

## 🔧 Troubleshooting

### Error: "No main manifest attribute"

**Solución:** Usar `mvn exec:java` en lugar de `java -jar`

### Scanner no lee input

**Solución:** Agregar `-Dexec.cleanupDaemonThreads=false` (ya configurado en pom.xml)

### Tests fallan después de cambios

**Verificar:**

```bash
mvn clean test
```

Si todos pasan → OK (no se afectó funcionalidad core)

---

## 📞 Contacto y Soporte

**Repositorio:** `D:\Usuario\Desktop\TPPROGRA2`  
**Tests:** `mvn test` (39 tests ✅)  
**Runner:** `mvn exec:java`  
**Documentación:** `docs/MANUAL_TESTS.md`

---

**🎉 INSTALACIÓN EXITOSA - RUNNER LISTO PARA USAR 🎉**

**Próximos pasos:**

1. Ejecutar: `mvn exec:java`
2. Opción `1` → Cargar `demo.json`
3. Opción `2` → Ver snapshot completo
4. Explorar menú según casos de `docs/MANUAL_TESTS.md`

---

**Fecha:** 24 de febrero de 2026  
**Versión:** 1.0 - Runner interactivo completo  
**Estado:** ✅ Listo para defensa
