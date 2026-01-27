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