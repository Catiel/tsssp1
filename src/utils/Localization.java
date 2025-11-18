package utils;

public final class Localization {
    private Localization() { }

    public static String getLocationDisplayName(String name) {
        if (name == null) {
            return "";
        }

        return switch (name) {
            case "DOCK" -> "Muelle";
            case "STOCK" -> "Inventario";
            case "Almacen_M1" -> "Almacen M1";
            case "M1" -> "Maquina 1";
            case "Almacen_M2" -> "Almacen M2";
            case "M2" -> "Maquina 2";
            case "Almacen_M3" -> "Almacen M3";
            case "M3" -> "Maquina 3";
            default -> name;
        };
    }
}
