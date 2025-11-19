package core; // Declaración del paquete core donde se encuentran las clases centrales de la simulación

import java.time.Duration; // Importa Duration para representar duraciones de tiempo
import java.time.LocalDateTime; // Importa LocalDateTime para manejar fechas y horas sin zona horaria
import java.time.format.DateTimeFormatter; // Importa DateTimeFormatter para formatear fechas/horas

public class SimulationClock { // Declaración de clase pública SimulationClock que maneja el tiempo de la simulación
    private double currentTime; // Variable privada que almacena el tiempo actual de simulación en horas (puede tener decimales)
    private LocalDateTime startDateTime; // Variable privada que almacena la fecha/hora de inicio real de la simulación
    private static final DateTimeFormatter TIME_FORMAT = // Constante estática final que define el formato de visualización de fecha/hora
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Formato: año-mes-día hora:minuto:segundo

    public SimulationClock() { // Constructor público que inicializa el reloj de simulación
        this.currentTime = 0.0; // Inicializa el tiempo de simulación en 0.0 horas
        this.startDateTime = LocalDateTime.of(2025, 1, 6, 6, 0); // Establece fecha/hora de inicio: Lunes 6 de enero de 2025 a las 6:00 AM
    }

    public void reset() { // Método público que reinicia el reloj de simulación al tiempo 0
        this.currentTime = 0.0; // Reinicia el tiempo actual a 0.0 horas
    }

    public void advanceTo(double newTime) { // Método público que avanza el reloj a un tiempo específico (no retrocede)
        if (newTime >= currentTime) { // Verifica si el nuevo tiempo es mayor o igual al tiempo actual
            this.currentTime = newTime; // Actualiza el tiempo actual al nuevo tiempo
        }
    }

    public void advance(double hours) { // Método público que avanza el reloj una cantidad específica de horas
        this.currentTime += hours; // Incrementa el tiempo actual sumando las horas recibidas
    }

    public double getCurrentTime() { // Método público getter que retorna el tiempo actual de simulación
        return currentTime; // Retorna el tiempo actual en horas
    }

    public LocalDateTime getCurrentDateTime() { // Método público que calcula y retorna la fecha/hora real actual basada en el tiempo de simulación
        long totalMinutes = (long)(currentTime * 60); // Convierte el tiempo en horas a minutos totales (multiplica por 60)
        return startDateTime.plusMinutes(totalMinutes); // Suma los minutos calculados a la fecha/hora de inicio y retorna el resultado
    }

    public String getFormattedTime() { // Método público que retorna la fecha/hora actual formateada como string
        return getCurrentDateTime().format(TIME_FORMAT); // Formatea la fecha/hora actual usando el formato definido y la retorna
    }

    public String getFormattedSimulationTime() { // Método público que retorna el tiempo de simulación formateado con contexto (semana, día, hora)
        int weeks = (int)(currentTime / 168.0); // Calcula el número de semanas completas dividiendo por 168 horas/semana
        double remainingHours = currentTime % 168.0; // Calcula las horas restantes después de las semanas completas usando módulo 168
        int days = (int)(remainingHours / 24.0); // Calcula el día de la semana dividiendo horas restantes por 24
        double hourOfDay = remainingHours % 24.0; // Calcula la hora del día usando módulo 24
        int hours = (int)hourOfDay; // Extrae la parte entera de la hora del día (horas completas)
        int minutes = (int)((hourOfDay - hours) * 60); // Calcula los minutos multiplicando la parte decimal por 60

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}; // Array con nombres abreviados de días de la semana
        String dayName = dayNames[days]; // Obtiene el nombre del día usando el índice calculado

        return String.format("Week %d - %s - %02d:%02d (%.2f hrs total)", // Retorna string formateado con toda la información
            weeks + 1, dayName, hours, minutes, currentTime); // Semana (base 1), día, hora:minuto (2 dígitos), total de horas (2 decimales)
    }

    public int getWeek() { // Método público que retorna el número de semana actual de la simulación (base 1)
        return (int)(currentTime / 168.0) + 1; // Divide tiempo por 168 horas/semana y suma 1 (las semanas comienzan en 1, no en 0)
    }

    public int getDayOfWeek() { // Método público que retorna el día de la semana (0=Lunes, 6=Domingo)
        return (int)((currentTime % 168.0) / 24.0); // Obtiene horas de la semana actual con módulo 168, luego divide por 24 para obtener día
    }

    public int getHourOfDay() { // Método público que retorna la hora del día (0-23)
        return (int)(currentTime % 24.0); // Aplica módulo 24 al tiempo actual para obtener hora del día
    }

    public int getMinuteOfHour() { // Método público que retorna el minuto de la hora actual (0-59)
        double hourOfDay = currentTime % 24.0; // Calcula la hora del día con decimales usando módulo 24
        int hours = (int)hourOfDay; // Extrae las horas completas (parte entera)
        return (int)((hourOfDay - hours) * 60); // Multiplica la parte decimal (fracción de hora) por 60 para obtener minutos
    }

    public String getDayName() { // Método público que retorna el nombre completo del día de la semana
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}; // Array con nombres completos de días en inglés
        return dayNames[getDayOfWeek()]; // Retorna el nombre del día usando el índice obtenido de getDayOfWeek()
    }

    public String getDayNameShort() { // Método público que retorna el nombre abreviado del día de la semana
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}; // Array con nombres abreviados de 3 letras
        return dayNames[getDayOfWeek()]; // Retorna el nombre abreviado usando el índice obtenido de getDayOfWeek()
    }

    public Duration getElapsedTime() { // Método público que calcula y retorna la duración transcurrida como objeto Duration
        return Duration.between(startDateTime, getCurrentDateTime()); // Calcula la duración entre la fecha/hora de inicio y la actual
    }

    public String getElapsedTimeFormatted() { // Método público que retorna el tiempo transcurrido formateado como string legible
        Duration elapsed = getElapsedTime(); // Obtiene la duración transcurrida
        long days = elapsed.toDays(); // Extrae los días completos de la duración
        long hours = elapsed.toHours() % 24; // Extrae las horas (módulo 24 para obtener solo las horas después de los días)
        long minutes = elapsed.toMinutes() % 60; // Extrae los minutos (módulo 60 para obtener solo los minutos después de las horas)

        return String.format("%d days, %d hours, %d minutes", days, hours, minutes); // Retorna string formateado con días, horas y minutos
    }

    @Override // Anotación que indica sobrescritura del método toString de Object
    public String toString() { // Método público que retorna una representación en string del reloj de simulación
        return getFormattedSimulationTime(); // Retorna el tiempo de simulación formateado con contexto completo
    }
}
