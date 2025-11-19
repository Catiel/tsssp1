package statistics; // Declaración del paquete statistics donde se encuentran las clases de estadísticas

import model.Valve; // Importa la clase Valve para acceder a los tipos de válvulas
import utils.Config; // Importa la clase Config para acceder a la configuración de escalas y capacidades

import java.util.*; // Importa todas las clases de utilidades de Java (Map, List, HashMap, ArrayList, Locale)
import java.util.concurrent.ConcurrentHashMap; // Importa ConcurrentHashMap para mapas thread-safe

public class Statistics { // Declaración de la clase pública Statistics que centraliza todas las estadísticas de la simulación
    private Map<Valve.Type, EntityStats> entityStats; // Mapa thread-safe que asocia cada tipo de válvula con sus estadísticas
    private Map<String, LocationStats> locationStats; // Mapa thread-safe que asocia cada locación con sus estadísticas
    private ResourceStats craneStats; // Objeto que almacena las estadísticas del recurso grúa
    private Map<Integer, Integer> arrivalsPerWeek; // Mapa thread-safe que cuenta arribos por semana (clave=semana, valor=cantidad)
    private Map<Integer, Integer> completionsPerArrivalWeek; // Mapa thread-safe que cuenta completaciones por semana de arribo

    public Statistics() { // Constructor que inicializa todas las estructuras de estadísticas
        entityStats = new ConcurrentHashMap<>(); // Inicializa mapa concurrente de estadísticas de entidades vacío
        locationStats = new ConcurrentHashMap<>(); // Inicializa mapa concurrente de estadísticas de locaciones vacío
        craneStats = new ResourceStats("Grua"); // Crea objeto de estadísticas para el recurso grúa
        arrivalsPerWeek = new ConcurrentHashMap<>(); // Inicializa mapa concurrente de arribos por semana vacío
        completionsPerArrivalWeek = new ConcurrentHashMap<>(); // Inicializa mapa concurrente de completaciones por semana de arribo vacío

        for (Valve.Type type : Valve.Type.values()) { // Itera sobre todos los tipos de válvulas definidos en el enum
            entityStats.put(type, new EntityStats(type)); // Crea y almacena objeto EntityStats para cada tipo de válvula
        }
    }

    public void recordArrival(Valve valve) { // Método público que registra el arribo de una válvula al sistema
        entityStats.get(valve.getType()).recordArrival(valve.getArrivalTime()); // Registra el arribo en las estadísticas del tipo de válvula correspondiente
        int week = getWeekIndex(valve.getArrivalTime()); // Calcula el índice de semana basado en el tiempo de arribo
        arrivalsPerWeek.merge(week, 1, Integer::sum); // Incrementa el contador de arribos para esa semana (merge suma 1 al valor existente o crea entrada con 1)
    }

    public void recordCompletion(Valve valve, double completionTime) { // Método público que registra la completación de una válvula
        EntityStats stats = entityStats.get(valve.getType()); // Obtiene las estadísticas del tipo de válvula
        stats.recordCompletion(completionTime); // Registra la completación con su tiempo
        stats.addTimeInSystem(valve.getTotalTimeInSystem(completionTime)); // Acumula el tiempo total que la válvula estuvo en el sistema
        stats.addProcessingTime(valve.getTotalProcessingTime()); // Acumula el tiempo total de procesamiento de la válvula
        stats.addMovementTime(valve.getTotalMovementTime()); // Acumula el tiempo total de movimiento/tránsito de la válvula
        stats.addWaitingTime(valve.getTotalWaitingTime()); // Acumula el tiempo total de espera de la válvula
        stats.addBlockedTime(valve.getTotalBlockedTime()); // Acumula el tiempo total bloqueado de la válvula

        int arrivalWeek = getWeekIndex(valve.getArrivalTime()); // Calcula la semana de arribo de la válvula
        completionsPerArrivalWeek.merge(arrivalWeek, 1, Integer::sum); // Incrementa el contador de completaciones para la semana de arribo
    }

    public void updateLocationStats(String locationName, int contents, // Método público que actualiza las estadísticas de una locación - Parámetro 1: nombre de la locación
                                    double utilization, double time) { // Parámetro 2: utilización actual, Parámetro 3: tiempo de simulación
        locationStats.putIfAbsent(locationName, new LocationStats(locationName)); // Crea objeto LocationStats si no existe para esta locación
        locationStats.get(locationName).update(contents, utilization, time); // Actualiza las estadísticas de la locación con los valores actuales
    }

    public void updateCraneStats(int units, // Método público que actualiza las estadísticas de la grúa - Parámetro 1: número de unidades
                                 double scheduledHours, // Parámetro 2: horas programadas
                                 double totalWorkMinutes, // Parámetro 3: minutos totales trabajados
                                 int trips, // Parámetro 4: número de viajes
                                 double avgHandleMinutes, // Parámetro 5: tiempo promedio de manejo
                                 double avgTravelMinutes, // Parámetro 6: tiempo promedio de viaje
                                 double avgParkMinutes, // Parámetro 7: tiempo promedio estacionado
                                 double blockedPercent, // Parámetro 8: porcentaje bloqueado
                                 double utilization, // Parámetro 9: utilización actual
                                 double time) { // Parámetro 10: tiempo de simulación
        craneStats.update(units, scheduledHours, totalWorkMinutes, trips, // Llama al método update de ResourceStats con todos los parámetros
            avgHandleMinutes, avgTravelMinutes, avgParkMinutes, blockedPercent, // (continuación de parámetros)
            utilization, time); // (continuación de parámetros)
    }

    public EntityStats getEntityStats(Valve.Type type) { // Método público que retorna las estadísticas de un tipo específico de válvula
        return entityStats.get(type); // Retorna el objeto EntityStats correspondiente al tipo solicitado
    }

    public LocationStats getLocationStats(String name) { // Método público que retorna las estadísticas de una locación específica
        return locationStats.get(name); // Retorna el objeto LocationStats correspondiente al nombre solicitado
    }

    public LocationStats getOrCreateLocationStats(String name) { // Método público que obtiene o crea las estadísticas de una locación
        return locationStats.computeIfAbsent(name, LocationStats::new); // Retorna LocationStats existente o crea uno nuevo si no existe
    }

    public ResourceStats getCraneStats() { // Método público que retorna las estadísticas de la grúa
        return craneStats; // Retorna el objeto ResourceStats de la grúa
    }

    public Map<Valve.Type, EntityStats> getAllEntityStats() { // Método público que retorna todas las estadísticas de entidades
        return new HashMap<>(entityStats); // Retorna una copia del mapa de estadísticas de entidades
    }

    public Map<String, LocationStats> getAllLocationStats() { // Método público que retorna todas las estadísticas de locaciones
        return new HashMap<>(locationStats); // Retorna una copia del mapa de estadísticas de locaciones
    }

    public Map<Integer, Integer> getArrivalsPerWeek() { // Método público que retorna el mapa de arribos por semana
        return new HashMap<>(arrivalsPerWeek); // Retorna una copia del mapa de arribos por semana
    }

    public Map<Integer, Integer> getCompletionsPerArrivalWeek() { // Método público que retorna el mapa de completaciones por semana de arribo
        return new HashMap<>(completionsPerArrivalWeek); // Retorna una copia del mapa de completaciones por semana de arribo
    }

    public int getArrivalsForWeek(int week) { // Método público que retorna el número de arribos de una semana específica
        return arrivalsPerWeek.getOrDefault(week, 0); // Retorna el valor almacenado o 0 si no existe entrada para esa semana
    }

    public int getCompletionsForArrivalWeek(int week) { // Método público que retorna el número de completaciones para una semana de arribo específica
        return completionsPerArrivalWeek.getOrDefault(week, 0); // Retorna el valor almacenado o 0 si no existe entrada para esa semana
    }

    public String generateReport(double currentTime) { // Método público que genera un reporte completo formateado de todas las estadísticas
        StringBuilder sb = new StringBuilder(); // Crea StringBuilder para construir el reporte eficientemente
        sb.append("╔═══════════════════════════════════════════════════════════╗\n"); // Agrega línea superior del encabezado con caracteres box-drawing
        sb.append("║        INFORME DE LA SIMULACION DE VALVULAS               ║\n"); // Agrega título centrado del reporte
        sb.append("╚═══════════════════════════════════════════════════════════╝\n\n"); // Agrega línea inferior del encabezado

        sb.append(String.format("Tiempo Simulado: %.2f horas (Semana %d)\n\n", // Agrega tiempo simulado formateado con 2 decimales
            currentTime, (int)(currentTime/168) + 1)); // Calcula y muestra el número de semana dividiendo por 168 horas

        sb.append("┌─────────────────────────────────────────────────────────┐\n"); // Agrega línea superior de sección de entidades
        sb.append("│  ESTADISTICAS DE ENTIDADES                                │\n"); // Agrega título de sección de entidades
        sb.append("├─────────────────────────────────────────────────────────┤\n"); // Agrega línea separadora
        Config config = Config.getInstance(); // Obtiene instancia singleton de la configuración
        for (Valve.Type type : Valve.Type.values()) { // Itera sobre todos los tipos de válvulas
            EntityStats stats = entityStats.get(type); // Obtiene las estadísticas del tipo actual
            if (stats == null) { // Verifica si no existen estadísticas para este tipo
                continue; // Salta a la siguiente iteración si no hay estadísticas
            }

            double systemMinutes = stats.getAvgTimeInSystem() * 60.0; // Convierte tiempo promedio en sistema de horas a minutos
            double movementMinutes = stats.getAvgMovementTime() * 60.0; // Convierte tiempo promedio de movimiento de horas a minutos
            double waitingMinutes = stats.getAvgWaitingTime() * 60.0; // Convierte tiempo promedio de espera de horas a minutos
            double processingMinutes = stats.getAvgProcessingTime() * 60.0; // Convierte tiempo promedio de procesamiento de horas a minutos
            double blockedMinutes = stats.getAvgBlockedTime() * 60.0; // Convierte tiempo promedio bloqueado de horas a minutos

            systemMinutes *= config.getEntityTimeScale(type, "system", 1.0); // Aplica escala configurada para tiempo en sistema
            movementMinutes *= config.getEntityTimeScale(type, "movement", 1.0); // Aplica escala configurada para tiempo de movimiento
            waitingMinutes *= config.getEntityTimeScale(type, "waiting", 1.0); // Aplica escala configurada para tiempo de espera
            processingMinutes *= config.getEntityTimeScale(type, "processing", 1.0); // Aplica escala configurada para tiempo de procesamiento
            blockedMinutes *= config.getEntityTimeScale(type, "blocked", 1.0); // Aplica escala configurada para tiempo bloqueado

            sb.append(String.format(Locale.ROOT, // Agrega línea formateada con estadísticas del tipo (usa ROOT para formato consistente)
                "%-12s | Salidas: %6d | En Sistema: %4d | T Sistema: %8.2f min | T Movimiento: %8.2f min | T Espera: %8.2f min | T Operacion: %8.2f min | T Bloqueo: %8.2f min\n", // Formato con alineación específica
                type.getDisplayName(), // Nombre del tipo alineado a izquierda en 12 caracteres
                stats.getTotalCompleted(), // Total de válvulas completadas alineado en 6 dígitos
                stats.getCurrentInSystem(), // Válvulas actualmente en sistema alineado en 4 dígitos
                systemMinutes, // Tiempo en sistema con 2 decimales alineado en 8 caracteres
                movementMinutes, // Tiempo de movimiento con 2 decimales alineado en 8 caracteres
                waitingMinutes, // Tiempo de espera con 2 decimales alineado en 8 caracteres
                processingMinutes, // Tiempo de procesamiento con 2 decimales alineado en 8 caracteres
                blockedMinutes)); // Tiempo bloqueado con 2 decimales alineado en 8 caracteres
        }

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n"); // Agrega línea superior de sección de ubicaciones
        sb.append("│  ESTADISTICAS DE UBICACIONES                              │\n"); // Agrega título de sección de ubicaciones
        sb.append("├─────────────────────────────────────────────────────────┤\n"); // Agrega línea separadora

        List<LocationStats> sortedStats = new ArrayList<>(locationStats.values()); // Crea lista con todas las estadísticas de locaciones
        sortedStats.sort((a, b) -> { // Define comparador personalizado para ordenar las estadísticas
            String nameA = a.getName(); // Obtiene nombre de la primera locación
            String nameB = b.getName(); // Obtiene nombre de la segunda locación

            boolean aIsUnit = nameA.matches("M[123]\\.\\d+"); // Verifica si es unidad individual (ej: M1.1, M2.3)
            boolean bIsUnit = nameB.matches("M[123]\\.\\d+"); // Verifica si es unidad individual
            if (aIsUnit || bIsUnit) { // Si al menos una es unidad individual
                if (aIsUnit && bIsUnit) return 0; // Si ambas son unidades, mantiene orden
                return aIsUnit ? 1 : -1; // Mueve unidades al final (1 = después, -1 = antes)
            }

            boolean aIsAggregate = nameA.matches("M[123]"); // Verifica si es máquina agregada (M1, M2, M3)
            boolean bIsAggregate = nameB.matches("M[123]"); // Verifica si es máquina agregada
            if (aIsAggregate != bIsAggregate) { // Si solo una es agregada
                return aIsAggregate ? -1 : 1; // Mueve agregadas al principio (-1 = antes)
            }
            if (aIsAggregate) { // Si ambas son agregadas
                return nameA.compareTo(nameB); // Ordena alfabéticamente entre ellas
            }

            boolean aIsAlmacen = nameA.startsWith("Almacen_"); // Verifica si es almacén
            boolean bIsAlmacen = nameB.startsWith("Almacen_"); // Verifica si es almacén
            if (aIsAlmacen != bIsAlmacen) { // Si solo una es almacén
                return aIsAlmacen ? -1 : 1; // Mueve almacenes después de agregadas pero antes del resto
            }
            if (aIsAlmacen) { // Si ambas son almacenes
                return nameA.compareTo(nameB); // Ordena alfabéticamente entre ellas
            }

            return nameA.compareTo(nameB); // Ordena alfabéticamente el resto de locaciones
        });

        for (LocationStats stats : sortedStats) { // Itera sobre las estadísticas ordenadas
            String name = stats.getName(); // Obtiene el nombre de la locación
            if (name.matches("M[123]\\.\\d+")) { // Verifica si es unidad individual
                continue; // Salta esta entrada (no la muestra en el reporte)
            }
            sb.append(stats.getReport()).append("\n"); // Agrega el reporte de la locación al StringBuilder
        }

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n"); // Agrega línea superior de sección de recursos
        sb.append("│  ESTADISTICAS DE RECURSOS                                 │\n"); // Agrega título de sección de recursos
        sb.append("├─────────────────────────────────────────────────────────┤\n"); // Agrega línea separadora
        sb.append(craneStats.getReport()).append("\n"); // Agrega el reporte de estadísticas de la grúa

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n"); // Agrega línea superior de sección de arribos vs completados
        sb.append("│  ARRIBOS VS COMPLETADOS POR SEMANA DE ARRIBO              │\n"); // Agrega título de sección
        sb.append("├─────────────────────────────────────────────────────────┤\n"); // Agrega línea separadora
        int maxWeek = arrivalsPerWeek.keySet().stream() // Crea stream de las claves (semanas) del mapa
            .mapToInt(Integer::intValue) // Convierte a stream de int primitivos
            .max() // Encuentra el valor máximo
            .orElse(0); // Retorna 0 si no hay elementos
        for (int week = 1; week <= maxWeek; week++) { // Itera desde la semana 1 hasta la última semana con datos
            int arrivals = getArrivalsForWeek(week); // Obtiene el número de arribos de esta semana
            int completions = getCompletionsForArrivalWeek(week); // Obtiene el número de completaciones para arribos de esta semana
            sb.append(String.format("Semana %02d | Llegadas: %3d | Completadas (segun arribo): %3d\n", // Formatea línea con semana, llegadas y completadas
                week, arrivals, completions)); // Inserta semana (2 dígitos), arribos (3 dígitos), completaciones (3 dígitos)
        }

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n"); // Agrega línea superior de sección de análisis
        sb.append("│  ANALISIS DE CUELLOS DE BOTELLA                           │\n"); // Agrega título de análisis de cuellos de botella
        sb.append("├─────────────────────────────────────────────────────────┤\n"); // Agrega línea separadora

        double maxUtil = 0; // Inicializa variable para almacenar la utilización máxima encontrada
        String bottleneck = "Ninguno"; // Inicializa variable para el nombre del cuello de botella (por defecto "Ninguno")

        LocationStats m1Stats = locationStats.get("M1"); // Obtiene estadísticas agregadas de la máquina M1
        LocationStats m2Stats = locationStats.get("M2"); // Obtiene estadísticas agregadas de la máquina M2
        LocationStats m3Stats = locationStats.get("M3"); // Obtiene estadísticas agregadas de la máquina M3

        if (m1Stats != null && m1Stats.getCurrentUtilization() > maxUtil) { // Verifica si M1 existe y tiene mayor utilización que el máximo actual
            maxUtil = m1Stats.getCurrentUtilization(); // Actualiza la utilización máxima con la de M1
            bottleneck = "Maquina 1"; // Establece M1 como cuello de botella
        }
        if (m2Stats != null && m2Stats.getCurrentUtilization() > maxUtil) { // Verifica si M2 existe y tiene mayor utilización que el máximo actual
            maxUtil = m2Stats.getCurrentUtilization(); // Actualiza la utilización máxima con la de M2
            bottleneck = "Maquina 2"; // Establece M2 como cuello de botella
        }
        if (m3Stats != null && m3Stats.getCurrentUtilization() > maxUtil) { // Verifica si M3 existe y tiene mayor utilización que el máximo actual
            maxUtil = m3Stats.getCurrentUtilization(); // Actualiza la utilización máxima con la de M3
            bottleneck = "Maquina 3"; // Establece M3 como cuello de botella
        }

        sb.append(String.format("Cuello Principal: %s (%.1f%% de utilizacion)\n", bottleneck, maxUtil)); // Agrega línea con el cuello de botella identificado y su utilización

        return sb.toString(); // Retorna el reporte completo como string
    }

    private int getWeekIndex(double time) { // Método privado que calcula el índice de semana basado en el tiempo de simulación
        return (int)Math.floor(time / 168.0) + 1; // Divide tiempo por 168 horas/semana, redondea hacia abajo y suma 1 (semanas comienzan en 1)
    }
}
