# 🚀 Solución Rápida - Cómo Ejecutar el Runner

## ✅ **COMANDO CORRECTO**

```bash
mvn exec:java
```

**¿Por qué funciona?**

- El `pom.xml` ya tiene `exec-maven-plugin` configurado con `mainClass=ar.uade.redsocial.DemoApp`
- Maven automáticamente incluye Gson y todas las dependencias en el classpath

---

## 📝 Explicación de los Errores Anteriores

### ❌ Error 1: `mvn -q exec:java -Dexec.mainClass=ar.uade.redsocial.DemoApp`

**Problema:** Guión incorrecto en `-Dexec.mainClass` (posiblemente copiado como guión largo: —)

**Solución:** Usar simplemente `mvn exec:java` (el mainClass ya está en pom.xml)

---

### ❌ Error 2: `java -cp "target/classes;target/dependency/*"`

**Problema:** Las dependencias NO están en `target/dependency/` hasta ejecutar:

```bash
mvn dependency:copy-dependencies
```

**Solución completa:**

```bash
# Paso 1: Compilar y copiar dependencias
mvn compile dependency:copy-dependencies

# Paso 2: Ejecutar con classpath manual
java -cp "target/classes;target/dependency/*" ar.uade.redsocial.DemoApp
```

---

## 🎯 Opciones de Ejecución

### **Opción 1: Maven Exec (RECOMENDADO ✅)**

```bash
mvn exec:java
```

**Ventajas:**

- ✅ Comando más corto
- ✅ Maven resuelve dependencias automáticamente
- ✅ No requiere copiar JARs
- ✅ Funciona en cualquier SO

---

### **Opción 2: Java con Classpath Manual**

```bash
# Solo primera vez o si hay cambios
mvn compile dependency:copy-dependencies

# Ejecutar
java -cp "target/classes;target/dependency/*" ar.uade.redsocial.DemoApp
```

**Ventajas:**

- ⚡ Arranque más rápido (después de compilar)
- 🔍 Control total del classpath

**Desventajas:**

- ❌ Requiere ejecutar `dependency:copy-dependencies` primero
- ❌ Comando más largo

---

### **Opción 3: Con Argumentos (JSON automático)**

```bash
mvn exec:java -Dexec.args="demo.json"
```

**Ventaja:** Carga `demo.json` automáticamente al iniciar

---

## 🧪 Verificación de Instalación

### 1. Tests Automatizados

```bash
mvn clean test
```

**Esperado:** `Tests run: 44, Failures: 0, Errors: 0`

### 2. Runner Interactivo

```bash
mvn exec:java
```

**Esperado:** Menú con 14 opciones

---

## 🐞 Troubleshooting

### Error: "No main manifest attribute"

**Solución:** Usar `mvn exec:java` (no `java -jar`)

### Warning: "Cliente 'A' tiene conexión a cliente inexistente: Fantasma"

**Causa:** Test unitario con datos de prueba  
**Impacto:** Ninguno en funcionalidad (solo warning en tests)

### Error: "NoClassDefFoundError: com/google/gson/GsonBuilder"

**Causa:** Dependencias no están en classpath  
**Solución:** Usar `mvn exec:java` (Maven resuelve automáticamente)

---

## 📚 Documentación Actualizada

Todos los archivos han sido corregidos con los comandos correctos:

- ✅ [README.md](README.md) - Comandos principales
- ✅ [docs/MANUAL_TESTS.md](docs/MANUAL_TESTS.md) - Casos de prueba
- ✅ [RUNNER_INSTALACION.md](RUNNER_INSTALACION.md) - Guía completa

---

## 🎉 ¡Listo para Usar!

```bash
# Paso único:
mvn exec:java

# Luego en el menú:
# 1. Opción 1 → Cargar demo.json
# 2. Opción 2 → Ver snapshot
# 3. Explorar otras operaciones
```

---

**Fecha:** 24 de febrero de 2026  
**Estado:** ✅ Todos los comandos corregidos y verificados
