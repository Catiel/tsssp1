package core;

import model.Valve;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase para manejar operaciones JOIN en el sistema de simulación de cervecería.
 * Una operación JOIN combina múltiples entidades de entrada para crear una nueva entidad de salida.
 * 
 * Ejemplos:
 * - COCCION: 1 GRANOS_CEBADA + 4 LUPULO → MOSTO
 * - FERMENTACION: 10L MOSTO + 2kg LEVADURA → CERVEZA
 * - EMPACADO: 6 BOTELLA_CERVEZA + 1 CAJA_VACIA → CAJA_CERVEZA
 */
public class JoinOperation {
    
    // Definición de operación JOIN
    private final String locationName;
    private final Map<Valve.Type, Integer> requiredEntities; // Tipo y cantidad requerida
    private final Valve.Type outputType; // Tipo de entidad resultante
    
    // Estado actual de acumulación
    private final Map<Valve.Type, Queue<Valve>> waitingEntities;
    
    public JoinOperation(String locationName, Map<Valve.Type, Integer> requiredEntities, Valve.Type outputType) {
        this.locationName = locationName;
        this.requiredEntities = new HashMap<>(requiredEntities);
        this.outputType = outputType;
        this.waitingEntities = new ConcurrentHashMap<>();
        
        // Inicializar colas para cada tipo de entidad requerida
        for (Valve.Type type : requiredEntities.keySet()) {
            waitingEntities.put(type, new LinkedList<>());
        }
    }
    
    /**
     * Agrega una entidad a la operación JOIN
     * @param entity Entidad a agregar
     * @return true si la operación está lista para ejecutarse
     */
    public synchronized boolean addEntity(Valve entity) {
        if (entity == null) {
            return false;
        }
        
        Valve.Type type = entity.getType();
        if (!requiredEntities.containsKey(type)) {
            return false; // Tipo no requerido para esta operación
        }
        
        Queue<Valve> queue = waitingEntities.get(type);
        queue.add(entity);
        
        return isReady();
    }
    
    /**
     * Verifica si la operación tiene todas las entidades necesarias
     */
    public synchronized boolean isReady() {
        for (Map.Entry<Valve.Type, Integer> entry : requiredEntities.entrySet()) {
            Valve.Type type = entry.getKey();
            int required = entry.getValue();
            int available = waitingEntities.get(type).size();
            
            if (available < required) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Ejecuta la operación JOIN: consume entidades y crea nueva entidad
     * @param currentTime Tiempo actual de simulación
     * @return Nueva entidad creada, o null si no está lista
     */
    public synchronized Valve execute(double currentTime) {
        if (!isReady()) {
            return null;
        }
        
        // Consumir entidades requeridas
        List<Valve> consumedEntities = new ArrayList<>();
        for (Map.Entry<Valve.Type, Integer> entry : requiredEntities.entrySet()) {
            Valve.Type type = entry.getKey();
            int required = entry.getValue();
            Queue<Valve> queue = waitingEntities.get(type);
            
            for (int i = 0; i < required; i++) {
                Valve consumed = queue.poll();
                if (consumed != null) {
                    consumedEntities.add(consumed);
                }
            }
        }
        
        // Crear nueva entidad del tipo de salida
        Valve newEntity = new Valve(outputType, currentTime);
        
        // Registrar transformación
        for (Valve consumed : consumedEntities) {
            consumed.setState(Valve.State.COMPLETED);
        }
        
        return newEntity;
    }
    
    /**
     * Obtiene el número de entidades esperando por tipo
     */
    public synchronized Map<Valve.Type, Integer> getWaitingCounts() {
        Map<Valve.Type, Integer> counts = new HashMap<>();
        for (Map.Entry<Valve.Type, Queue<Valve>> entry : waitingEntities.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }
    
    /**
     * Obtiene el nombre de la ubicación
     */
    public String getLocationName() {
        return locationName;
    }
    
    /**
     * Obtiene el tipo de entidad de salida
     */
    public Valve.Type getOutputType() {
        return outputType;
    }
    
    /**
     * Obtiene cuántas entidades de un tipo específico aún se necesitan
     */
    public synchronized int getNeededCount(Valve.Type type) {
        if (!requiredEntities.containsKey(type)) {
            return 0;
        }
        int required = requiredEntities.get(type);
        int available = waitingEntities.get(type).size();
        return Math.max(0, required - available);
    }
    
    /**
     * Reinicia la operación (útil para debugging o reinicio de simulación)
     */
    public synchronized void reset() {
        for (Queue<Valve> queue : waitingEntities.values()) {
            queue.clear();
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JoinOperation[").append(locationName).append("]: ");
        for (Map.Entry<Valve.Type, Integer> entry : requiredEntities.entrySet()) {
            int available = waitingEntities.get(entry.getKey()).size();
            sb.append(entry.getValue()).append(" ").append(entry.getKey())
              .append(" (").append(available).append("/").append(entry.getValue()).append(") + ");
        }
        sb.append(" → ").append(outputType);
        return sb.toString();
    }
}
