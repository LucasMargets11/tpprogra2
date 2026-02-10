package python;

public class App {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Ingrese su nombre ");
        String nombre = scanner.next();
        System.out.println("Hola " + nombre + ", bienvenido al mundo de Java!");
        scanner.close();
    }
}
/*
 * 
 * 
 * HashMap (acceso directo) → O(1) promedio
 * 
 * Uso típico:
 * 
 * buscar cliente por nombre / id
 * 
 * validar existencia rápido (“¿existe Ana?”)
 * 
 * evitar duplicados durante carga
 * 
 * Por qué importa: en redes sociales, la operación dominante es “buscar
 * usuario”. HashMap evita recorridos lineales.
 * 
 * 
 * TreeMap (ordenado por scoring) → O(log n) y rangos eficientes
 * 
 * Uso típico:
 * 
 * “buscar por scoring”
 * 
 * “buscar entre X e Y”
 * 
 * obtener resultados ordenados sin ordenar manualmente cada vez
 * 
 * TreeMap mantiene el orden internamente (árbol balanceado), entonces:
 * 
 * insertar cuesta O(log n)
 * 
 * buscar rangos cuesta O(log n + k) (k resultados)
 * 
 * Por qué importa: sin índice ordenado, cada búsqueda por rango sería O(n)
 * recorriendo todos los clientes.
 * 
 * 
 * ArrayDeque (cola FIFO real) → O(1)
 * 
 * Uso típico:
 * 
 * solicitudes de seguimiento pendientes
 * 
 * “primero que entra, primero que sale”
 * 
 * Por qué importa: una cola con ArrayDeque es eficiente y semánticamente
 * correcta para FIFO. Evita costos de estructuras equivocadas.
 * 
 * 
 * Historial de acciones como pila (undo) → O(1)
 * 
 * Push cuando ocurre una acción
 * 
 * Pop para deshacer la última
 * 
 * Por qué importa: deshacer secuencial se modela naturalmente como pila. No hay
 * que buscar la “última acción” cada vez.
 * 
 * 
 * Cómo explicar el “Undo” sin entrar demasiado en Java
 * 
 * “Cada operación relevante registra una Action en un historial. Undo toma la
 * última acción (pila) y ejecuta la operación inversa. Esto permite revertir
 * cambios de forma consistente y en orden temporal.”
 * 
 * Cierre ideal para presentar con demo (lo que ya hiciste)
 * 
 * “Para demostrarlo, agregamos DemoApp que carga un JSON y muestra: cantidad de
 * clientes, búsquedas por scoring, búsqueda por nombre, comportamiento FIFO y
 * undo. Y además los tests automatizados validan 8 casos clave.”
 */