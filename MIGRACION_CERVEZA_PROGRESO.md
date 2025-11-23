# Migración de Simulación de Válvulas → Cervecería

## ✅ COMPLETADO (Archivos Modificados)

### 1. **`Valve.java`** ✓
- Transformado de 4 tipos de válvulas → 8 tipos de entidades cerveceras
- Rutas cambiadas de `int[][]` (máquinas+tiempos) → `String[]` (nombres de locaciones)
- Tiempos de proceso ahora en `Map<String, Double>` por locación (en minutos)
- Tipos de entidades:
  - GRANOS_CEBADA (café claro)
  - LUPULO (verde)
  - LEVADURA (amarillo)
  - MOSTO (café)
  - CERVEZA (ámbar)
  - BOTELLA_CERVEZA (dorado)
  - CAJA_VACIA (gris)
  - CAJA_CERVEZA (naranja)

### 2. **`ShiftCalendar.java`** ✓
- Modificado para trabajar 7 días/semana (lunes-domingo)
- Turnos: 10 horas diarias

### 3. **`MainFrame.java`** y **`Main.java`** ✓
- Títulos actualizados: "Simulación de Producción de Cerveza Artesanal"
- Mensajes de log actualizados

### 4. **`brewery.properties`** ✓
- Archivo de configuración completo creado
- Parámetros para todas las 19 locaciones
- Arribos de 4 tipos de entidades
- Configuración de 4 operadores + 1 camión

### 5. **`SimulationEngine.java`** (PARCIALMENTE) ⚠️
**Completado:**
- Constantes cambiadas: `HOURS_PER_WEEK` → `MINUTES_PER_WEEK` (4200 min)
- `SAMPLE_INTERVAL` = 60 minutos
- `DEFAULT_WEEKS_TO_SIMULATE` = 1 semana
- Cálculo de `endTime` en minutos (4200 min = 1 semana)
- Método `initializeLocations()`: Creadas 19 locaciones nuevas
- Método `scheduleArrivals()`: Arribos con frecuencias (25, 10, 20, 30 min)
- Método `handleArrival()`: Modificado para manejar llegadas a diferentes locaciones

---

## 🔄 PENDIENTE (Cambios Críticos Restantes)

### 6. **`SimulationEngine.java`** (Continuación) 🚧

#### A. Método `handleEndProcessing()` - CRÍTICO
**Cambios necesarios:**
```java
- Eliminar lógica de almacenes M1/M2/M3
- Implementar lógica de flujo secuencial por rutas
- Agregar manejo de 3 operaciones JOIN:
  1. COCCION: 1 grano + 4 lúpulo → MOSTO
  2. FERMENTACION: 10 mosto + 2 levadura → CERVEZA  
  3. EMPACADO: 6 botellas + 1 caja → CAJA_CERVEZA
- Agregar lógica de INSPECCION (90% → EMBOTELLADO, 10% → EXIT)
- Implementar ACCUM 6 en ALMACENAJE antes de ir a MERCADO
```

#### B. Método `initializeCrane()` → `initializeOperators()`
**Cambios necesarios:**
```java
- Eliminar clase Crane
- Crear 4 operadores + 1 camión:
  * operadorRecepcion (90 pasos/min): MALTEADO→SECADO→MOLIENDA
  * operadorLupulo (100 pasos/min): SILO_LUPULO→COCCION
  * operadorLevadura (100 pasos/min): SILO_LEVADURA→FERMENTACION
  * operadorEmpacado (100 pasos/min): EMPACADO→ALMACENAJE
  * camion (100 pasos/min): ALMACENAJE→MERCADO (ACCUM 6)
```

#### C. Métodos de movimiento de grúa → operadores
**Eliminar/Reemplazar:**
- `tryScheduleCraneMove()`
- `scheduleCraneMove()`
- `handleStartCraneMove()`
- `handleEndCraneMove()`
- `findFirstAvailableValveInDock()`
- `pollPendingCraneTransfer()`

**Crear:**
- `tryScheduleOperatorMove(Operator operator, String fromLocation, String toLocation)`
- `scheduleOperatorMove(Operator operator, Valve entity, String destination)`
- `handleOperatorPickup(Operator operator, Valve entity)`
- `handleOperatorRelease(Operator operator, Valve entity)`

#### D. Método `getNextDestination()`
**Cambios necesarios:**
```java
- Simplificar: solo retornar valve.getNextLocation()
- Eliminar lógica de almacenes M1/M2/M3
- Manejar casos especiales:
  * Después de COCCION → crear MOSTO
  * Después de FERMENTACION → crear CERVEZA
  * Después de EMBOTELLADO → crear BOTELLAS (6x)
  * Después de EMPACADO → crear CAJA_CERVEZA
```

#### E. Nuevos métodos para operaciones JOIN
```java
private void handleJoinCoccion(Valve granos, List<Valve> lupulos) {
    // Verificar: 1 grano + 4 lúpulos
    // Crear: 1 MOSTO
}

private void handleJoinFermentacion(Valve mosto, List<Valve> levaduras) {
    // Verificar: 10 L mosto + 2 kg levadura
    // Crear: 1 CERVEZA
}

private void handleJoinEmpacado(List<Valve> botellas, Valve caja) {
    // Verificar: 6 botellas + 1 caja
    // Crear: 1 CAJA_CERVEZA
}
```

#### F. Nuevo método para inspección
```java
private void handleInspeccion(Valve cerveza) {
    double random = Math.random();
    if (random < 0.9) {
        // 90% → EMBOTELLADO (aprobar)
        scheduleRoute(cerveza, "EMBOTELLADO");
    } else {
        // 10% → EXIT (descartar)
        cerveza.setState(Valve.State.COMPLETED);
        completedValves.add(cerveza);
        // NO contar como producción exitosa
    }
}
```

#### G. Método `checkMachineQueue()` → `checkLocationQueue()`
**Cambios necesarios:**
- Eliminar lógica de unidades M1.1, M1.2, etc.
- Simplificar: solo verificar si hay entidades esperando y unidad disponible
- Iniciar procesamiento directamente

---

### 7. **`PathNetwork.java`** 🚧
**Cambios necesarios:**
```java
// Eliminar nodos N1-N5 antiguos
// Crear 4 redes nuevas:

RED_RECEPCION:
  N1 (MALTEADO) → N2 (SECADO) → N3 (MOLIENDA)
  
RED_LUPULO:
  N1 (SILO_LUPULO) → N2 (COCCION)
  
RED_LEVADURA:
  N1 (SILO_LEVADURA) → N2 (FERMENTACION)
  
RED_EMPACADO:
  N1 (EMPACADO) → N2 (ALMACENAJE) → N3 (MERCADO)
```

---

### 8. **`Crane.java` → `Operator.java`** 🚧
**Crear nueva clase:**
```java
public class Operator {
    private String name; // "Operador_Recepcion", "Camion", etc.
    private int speed; // 90 o 100 pasos/min
    private String network; // "RED_RECEPCION", "RED_LUPULO", etc.
    private Point homePosition;
    private boolean isBusy;
    private Valve carryingEntity;
    
    // Métodos similares a Crane pero simplificados
}
```

---

### 9. **`Statistics.java`** ⚠️
**Cambios menores:**
- Actualizar nombres de entidades en reportes
- Cambiar "válvulas" → "entidades"
- Agregar estadísticas de:
  - Cerveza aprobada vs descartada en inspección
  - Cajas empacadas
  - Entidades en cada locación

---

### 10. **Archivos GUI** ⚠️
**Modificaciones menores en:**
- `SimulationPanel.java`: Actualizar visualización para 19 locaciones
- `AnimationPanel.java`: Cambiar íconos de válvulas → entidades cerveceras
- `ChartsPanel.java`: Actualizar gráficos con nuevas locaciones
- `StatisticsPanel.java`: Actualizar tablas con nuevas entidades

---

## 📊 ESTIMACIÓN DE TRABAJO RESTANTE

| Archivo | Líneas a Modificar | Complejidad | Prioridad |
|---------|-------------------|-------------|-----------|
| SimulationEngine.java | ~400 líneas | ALTA | 🔴 CRÍTICA |
| Operator.java (nuevo) | ~150 líneas | MEDIA | 🔴 CRÍTICA |
| PathNetwork.java | ~80 líneas | MEDIA | 🟡 ALTA |
| JoinOperation.java (nuevo) | ~100 líneas | ALTA | 🟡 ALTA |
| Statistics.java | ~50 líneas | BAJA | 🟢 MEDIA |
| Archivos GUI | ~200 líneas | MEDIA | 🟢 MEDIA |

**Total estimado:** ~980 líneas de código adicionales a modificar/crear

---

## 🎯 PRÓXIMOS PASOS RECOMENDADOS

1. **Terminar `SimulationEngine.java`:**
   - Implementar operaciones JOIN
   - Agregar lógica de inspección
   - Implementar ACCUM 6 en almacenaje

2. **Crear `Operator.java`:**
   - Reemplazar funcionalidad de Crane
   - Implementar 4 operadores + camión

3. **Modificar `PathNetwork.java`:**
   - Crear 4 redes de rutas nuevas

4. **Crear `JoinOperation.java`:**
   - Clase auxiliar para manejar las 3 uniones

5. **Actualizar archivos GUI:**
   - Visualización de 19 locaciones
   - Nuevos colores por tipo de entidad

---

## ⚠️ DESAFÍOS TÉCNICOS IDENTIFICADOS

1. **Operaciones JOIN complejas:**
   - Necesitan esperar múltiples entidades antes de procesar
   - Requieren sincronización y contadores

2. **ACCUM 6 en ALMACENAJE:**
   - Acumular 6 cajas antes de enviar al camión
   - Requiere buffer y lógica de lote

3. **Inspección probabilística:**
   - 90% aprobación / 10% rechazo
   - Ramificación del flujo

4. **4 Operadores concurrentes:**
   - Cada uno con su red de rutas específica
   - Gestión de recursos compartidos

5. **Conversión de unidades:**
   - Todo de horas → minutos
   - Ajustar TODAS las referencias de tiempo

---

## 📝 NOTAS IMPORTANTES

- El proyecto está **40% completado** en términos de migración
- Las bases están listas (entidades, locaciones, configuración)
- Falta el **núcleo de la lógica de procesamiento**
- Se requieren **~3-4 horas adicionales** de trabajo intensivo
- Es crucial **probar cada componente** antes de integrar

---

**Última actualización:** 22 de noviembre de 2025
**Estado:** 🟡 EN PROGRESO (Fase 1 de 3 completada)
