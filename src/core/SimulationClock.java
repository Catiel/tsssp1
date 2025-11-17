package core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimulationClock {
    private double currentTime; // Time in hours
    private LocalDateTime startDateTime;
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SimulationClock() {
        this.currentTime = 0.0;
        this.startDateTime = LocalDateTime.of(2025, 1, 6, 6, 0); // Monday, Jan 6, 2025 at 6:00 AM
    }

    public void reset() {
        this.currentTime = 0.0;
    }

    public void advanceTo(double newTime) {
        if (newTime >= currentTime) {
            this.currentTime = newTime;
        }
    }

    public void advance(double hours) {
        this.currentTime += hours;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public LocalDateTime getCurrentDateTime() {
        long totalMinutes = (long)(currentTime * 60);
        return startDateTime.plusMinutes(totalMinutes);
    }

    public String getFormattedTime() {
        return getCurrentDateTime().format(TIME_FORMAT);
    }

    public String getFormattedSimulationTime() {
        int weeks = (int)(currentTime / 168.0);
        double remainingHours = currentTime % 168.0;
        int days = (int)(remainingHours / 24.0);
        double hourOfDay = remainingHours % 24.0;
        int hours = (int)hourOfDay;
        int minutes = (int)((hourOfDay - hours) * 60);

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String dayName = dayNames[days];

        return String.format("Week %d - %s - %02d:%02d (%.2f hrs total)",
            weeks + 1, dayName, hours, minutes, currentTime);
    }

    public int getWeek() {
        return (int)(currentTime / 168.0) + 1;
    }

    public int getDayOfWeek() {
        return (int)((currentTime % 168.0) / 24.0);
    }

    public int getHourOfDay() {
        return (int)(currentTime % 24.0);
    }

    public int getMinuteOfHour() {
        double hourOfDay = currentTime % 24.0;
        int hours = (int)hourOfDay;
        return (int)((hourOfDay - hours) * 60);
    }

    public String getDayName() {
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday",
                           "Friday", "Saturday", "Sunday"};
        return dayNames[getDayOfWeek()];
    }

    public String getDayNameShort() {
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        return dayNames[getDayOfWeek()];
    }

    public Duration getElapsedTime() {
        return Duration.between(startDateTime, getCurrentDateTime());
    }

    public String getElapsedTimeFormatted() {
        Duration elapsed = getElapsedTime();
        long days = elapsed.toDays();
        long hours = elapsed.toHours() % 24;
        long minutes = elapsed.toMinutes() % 60;

        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
    }

    @Override
    public String toString() {
        return getFormattedSimulationTime();
    }
}
