package model;

import utils.Config;
import utils.Logger;
import java.util.*;

public class ShiftCalendar {
    private final boolean[] workingHours;
    private static final int HOURS_PER_WEEK = 168;
    private final int shiftStartHour;
    private final int shiftEndHour;
    private final Set<Integer> workingDays;

    public ShiftCalendar() {
        Config config = Config.getInstance();

        this.shiftStartHour = config.getShiftStartHour();
        this.shiftEndHour = config.getShiftEndHour();
        this.workingDays = new HashSet<>();

        String[] dayNames = config.getWorkingDays();
        for (String day : dayNames) {
            workingDays.add(getDayIndex(day.trim()));
        }

        this.workingHours = new boolean[HOURS_PER_WEEK];
        initializeShifts();

        Logger.getInstance().info(String.format(
            "Shift calendar initialized: %d-%d hours, %d working days",
            shiftStartHour, shiftEndHour, workingDays.size()));
    }

    private int getDayIndex(String dayName) {
        Map<String, Integer> dayMap = new HashMap<>();
        dayMap.put("Monday", 0);
        dayMap.put("Tuesday", 1);
        dayMap.put("Wednesday", 2);
        dayMap.put("Thursday", 3);
        dayMap.put("Friday", 4);
        dayMap.put("Saturday", 5);
        dayMap.put("Sunday", 6);

        return dayMap.getOrDefault(dayName, 0);
    }

    private void initializeShifts() {
        for (int dayIndex : workingDays) {
            for (int hour = shiftStartHour; hour < shiftEndHour; hour++) {
                workingHours[dayIndex * 24 + hour] = true;
            }
        }
    }

    public boolean isWorkingTime(double time) {
        int weekHour = ((int) time) % HOURS_PER_WEEK;
        return workingHours[weekHour];
    }

    public double getNextWorkingTime(double currentTime) {
        int startHour = ((int) currentTime) % HOURS_PER_WEEK;

        for (int offset = 0; offset < HOURS_PER_WEEK; offset++) {
            int hour = (startHour + offset) % HOURS_PER_WEEK;
            if (workingHours[hour]) {
                return currentTime + offset;
            }
        }

        return currentTime; // Should never reach here
    }

    public String getDayName(double time) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday",
                        "Friday", "Saturday", "Sunday"};
        int day = ((int)(time / 24)) % 7;
        return days[day];
    }

    public String getDayNameShort(double time) {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int day = ((int)(time / 24)) % 7;
        return days[day];
    }

    public int getWeekNumber(double time) {
        return ((int)(time / HOURS_PER_WEEK)) + 1;
    }

    public int getDayOfWeek(double time) {
        return ((int)(time / 24)) % 7;
    }

    public int getHourOfDay(double time) {
        return ((int) time) % 24;
    }

    public int getMinuteOfHour(double time) {
        double hourOfDay = time % 24.0;
        int hours = (int)hourOfDay;
        return (int)((hourOfDay - hours) * 60);
    }

    public double getTotalWorkingHoursPerWeek() {
        int count = 0;
        for (boolean working : workingHours) {
            if (working) count++;
        }
        return count;
    }

    public boolean isWeekend(double time) {
        int day = getDayOfWeek(time);
        return day == 5 || day == 6; // Saturday or Sunday
    }

    @Override
    public String toString() {
        return String.format("ShiftCalendar[%d-%d hrs, %.0f working hrs/week]",
            shiftStartHour, shiftEndHour, getTotalWorkingHoursPerWeek());
    }
}
