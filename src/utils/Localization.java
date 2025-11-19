package utils; // Declaración del paquete utils donde se encuentran las clases de utilidades

public final class Localization { // Declaración de clase pública final Localization (final evita que sea extendida) que proporciona traducción de nombres
    private Localization() { } // Constructor privado vacío que previene instanciación externa (clase de utilidad estática)

    public static String getLocationDisplayName(String name) { // Método estático público que traduce nombres de locaciones de inglés a español
        if (name == null) { // Verifica si el nombre recibido es null
            return ""; // Retorna string vacío si el nombre es null para evitar NullPointerException
        }

        return switch (name) { // Expresión switch moderna (Java 14+) que evalúa el nombre y retorna la traducción correspondiente
            case "DOCK" -> "Dock"; // Si el nombre es "DOCK", retorna su traducción al español "Muelle"
            case "STOCK" -> "Stock"; // Si el nombre es "STOCK", retorna su traducción al español "Inventario"
            case "Almacen_M1" -> "Almacen M1"; // Si el nombre es "Almacen_M1", retorna formato legible "Almacen M1" (elimina guión bajo)
            case "M1" -> "Maquina 1"; // Si el nombre es "M1", retorna su traducción al español "Maquina 1"
            case "Almacen_M2" -> "Almacen M2"; // Si el nombre es "Almacen_M2", retorna formato legible "Almacen M2" (elimina guión bajo)
            case "M2" -> "Maquina 2"; // Si el nombre es "M2", retorna su traducción al español "Maquina 2"
            case "Almacen_M3" -> "Almacen M3"; // Si el nombre es "Almacen_M3", retorna formato legible "Almacen M3" (elimina guión bajo)
            case "M3" -> "Maquina 3"; // Si el nombre es "M3", retorna su traducción al español "Maquina 3"
            default -> name; // Para cualquier otro nombre no especificado, retorna el nombre original sin traducir
        }; // Cierre de la expresión switch
    } // Cierre del método getLocationDisplayName
} // Cierre de la clase Localization
