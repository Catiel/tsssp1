package model; // Declaración del paquete model donde se encuentra la clase

import utils.Config; // Importa la clase Config para acceder a la configuración de la aplicación
import utils.Logger; // Importa la clase Logger para registrar mensajes de log
import java.util.*; // Importa todas las clases de utilidades de Java (Map, Set, List, HashMap, HashSet, ArrayList)

public class ShiftCalendar { // Declaración de la clase pública ShiftCalendar que gestiona los turnos de trabajo semanales
    private final boolean[] workingHours; // Array final de 168 elementos (7 días × 24 horas) que indica si cada hora de la semana es laborable
    private static final int HOURS_PER_WEEK = 168; // Constante estática final que define el número de horas en una semana (7 × 24)
    private final Set<Integer> workingDays; // Conjunto final que almacena los índices de días laborables (0=Lunes, 6=Domingo)
    private final List<int[]> shiftBlocks; // Lista final que almacena bloques de turnos como arrays [horaInicio, horaFin]
    private final int shiftStartHour; // Variable final que almacena la hora de inicio del turno (ej: 8)
    private final int shiftEndHour; // Variable final que almacena la hora de fin del turno (ej: 17)

    public ShiftCalendar() { // Constructor que inicializa el calendario de turnos desde la configuración
        Config config = Config.getInstance(); // Obtiene la instancia singleton de la configuración

        this.shiftStartHour = config.getShiftStartHour(); // Obtiene y asigna la hora de inicio del turno desde la configuración
        this.shiftEndHour = config.getShiftEndHour(); // Obtiene y asigna la hora de fin del turno desde la configuración
        this.workingDays = new HashSet<>(); // Inicializa el conjunto de días laborables vacío
        this.shiftBlocks = new ArrayList<>(); // Inicializa la lista de bloques de turnos vacía

        // Para cerveza: trabajan 7 días a la semana
        for (int i = 0; i < 7; i++) {
            workingDays.add(i); // Agrega todos los días (0-6)
        }

        List<int[]> configuredBlocks = config.getShiftBlocks(); // Obtiene los bloques de turnos configurados
        if (configuredBlocks.isEmpty()) { // Verifica si no hay bloques configurados
            shiftBlocks.add(new int[]{shiftStartHour, shiftEndHour}); // Agrega un bloque por defecto con inicio y fin configurados
        } else { // Si hay bloques configurados
            shiftBlocks.addAll(configuredBlocks); // Agrega todos los bloques configurados a la lista
        }

        this.workingHours = new boolean[HOURS_PER_WEEK]; // Inicializa el array de 168 horas con valores false por defecto
        initializeShifts(); // Llama al método que marca las horas laborables en el array

        Logger.getInstance().info(String.format( // Registra mensaje informativo en el log
            "Shift calendar initialized: %d blocks, %d working days", // Formato del mensaje con placeholders
            shiftBlocks.size(), workingDays.size())); // Inserta número de bloques y días laborables
    }

    private int getDayIndex(String dayName) { // Método privado que convierte el nombre de un día a su índice (0-6)
        Map<String, Integer> dayMap = new HashMap<>(); // Crea mapa para asociar nombres de días con índices
        dayMap.put("Monday", 0); // Lunes = 0
        dayMap.put("Tuesday", 1); // Martes = 1
        dayMap.put("Wednesday", 2); // Miércoles = 2
        dayMap.put("Thursday", 3); // Jueves = 3
        dayMap.put("Friday", 4); // Viernes = 4
        dayMap.put("Saturday", 5); // Sábado = 5
        dayMap.put("Sunday", 6); // Domingo = 6

        return dayMap.getOrDefault(dayName, 0); // Retorna el índice del día o 0 (Lunes) si no se encuentra
    }

    private void initializeShifts() { // Método privado que inicializa el array workingHours marcando las horas laborables
        for (int dayIndex : workingDays) { // Itera sobre cada día laborable
            for (int[] block : shiftBlocks) { // Itera sobre cada bloque de turno
                int start = Math.max(0, Math.min(23, block[0])); // Limita la hora de inicio entre 0 y 23
                int end = Math.max(0, Math.min(24, block[1])); // Limita la hora de fin entre 0 y 24
                for (int hour = start; hour < end; hour++) { // Itera sobre cada hora del bloque
                    workingHours[dayIndex * 24 + hour] = true; // Marca la hora como laborable en el array (índice = día*24 + hora)
                }
            }
        }
    }

    public boolean isWorkingTime(double time) { // Método público que verifica si un tiempo dado es horario laborable
        int weekHour = ((int) time) % HOURS_PER_WEEK; // Calcula la hora de la semana (0-167) usando módulo 168
        return workingHours[weekHour]; // Retorna true si esa hora está marcada como laborable
    }

    public double getNextWorkingTime(double currentTime) { // Método público que encuentra el próximo horario laborable desde un tiempo dado
        int startHour = ((int) currentTime) % HOURS_PER_WEEK; // Calcula la hora de inicio de la semana usando módulo 168

        for (int offset = 0; offset < HOURS_PER_WEEK; offset++) { // Itera hasta una semana completa buscando hora laborable
            int hour = (startHour + offset) % HOURS_PER_WEEK; // Calcula la hora a verificar con wrap-around semanal
            if (workingHours[hour]) { // Verifica si esta hora es laborable
                return currentTime + offset; // Retorna el tiempo actual más el offset encontrado
            }
        }

        return currentTime; // Retorna el tiempo actual si no se encuentra (nunca debería llegar aquí si hay horas laborables)
    }

    public String getDayName(double time) { // Método público que retorna el nombre completo del día de la semana
        String[] days = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"}; // Array con nombres completos en español
        int day = ((int)(time / 24)) % 7; // Calcula el índice del día dividiendo por 24 y aplicando módulo 7
        return days[day]; // Retorna el nombre del día correspondiente al índice
    }

    public String getDayNameShort(double time) { // Método público que retorna el nombre abreviado del día de la semana
        String[] days = {"Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom"}; // Array con nombres abreviados de 3 letras
        int day = ((int)(time / 24)) % 7; // Calcula el índice del día dividiendo por 24 y aplicando módulo 7
        return days[day]; // Retorna el nombre abreviado del día correspondiente al índice
    }

    public int getWeekNumber(double time) { // Método público que calcula el número de semana (basado en 1)
        return ((int)(time / HOURS_PER_WEEK)) + 1; // Divide el tiempo por 168 horas y suma 1 para comenzar en semana 1
    }

    public int getDayOfWeek(double time) { // Método público que retorna el día de la semana (0-6)
        return ((int)(time / 24)) % 7; // Divide el tiempo por 24 para obtener días y aplica módulo 7
    }

    public int getHourOfDay(double time) { // Método público que retorna la hora del día (0-23)
        return ((int) time) % 24; // Convierte a entero y aplica módulo 24 para obtener hora del día
    }

    public int getMinuteOfHour(double time) { // Método público que retorna el minuto de la hora (0-59)
        double hourOfDay = time % 24.0; // Calcula la hora del día como double (ej: 14.5 = 14:30)
        int hours = (int)hourOfDay; // Extrae la parte entera (horas completas)
        return (int)((hourOfDay - hours) * 60); // Multiplica la parte decimal por 60 para obtener minutos
    }

    public double getTotalWorkingHoursPerWeek() { // Método público que calcula el total de horas laborables por semana
        int count = 0; // Inicializa contador en 0
        for (boolean working : workingHours) { // Itera sobre cada hora de la semana
            if (working) count++; // Incrementa el contador si la hora es laborable
        }
        return count; // Retorna el total de horas laborables
    }

    public int getFirstWorkingHourOfWeek() { // Método público que encuentra la primera hora laborable de la semana
        for (int i = 0; i < workingHours.length; i++) { // Itera sobre todas las horas de la semana
            if (workingHours[i]) { // Verifica si esta hora es laborable
                return i; // Retorna el índice de la primera hora laborable encontrada
            }
        }
        return 0; // Retorna 0 si no se encuentra ninguna hora laborable (caso por defecto)
    }

    public int getLastWorkingHourExclusive() { // Método público que encuentra la última hora laborable + 1 (exclusivo)
        for (int i = workingHours.length - 1; i >= 0; i--) { // Itera en reversa desde la última hora de la semana
            if (workingHours[i]) { // Verifica si esta hora es laborable
                return i + 1; // Retorna el índice + 1 (exclusivo) de la última hora laborable
            }
        }
        return workingHours.length; // Retorna 168 si no se encuentra ninguna hora laborable
    }

    public boolean isWeekend(double time) { // Método público que verifica si un tiempo dado es fin de semana
        int day = getDayOfWeek(time); // Obtiene el día de la semana (0-6)
        return day == 5 || day == 6; // Retorna true si es sábado (5) o domingo (6)
    }

    @Override // Anotación que indica sobrescritura del método toString de Object
    public String toString() { // Método público que retorna una representación en string del calendario
        return String.format("ShiftCalendar[%d blocks, %.0f working hrs/week]", // Formato con placeholders para bloques y horas
            shiftBlocks.size(), getTotalWorkingHoursPerWeek()); // Inserta número de bloques y total de horas laborables
    }
}
