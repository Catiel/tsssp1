package core;

import model.Valve;

public class Event implements Comparable<Event> {
    public enum Type {
        ARRIVAL,
        END_PROCESSING,
        START_CRANE_MOVE,
        END_CRANE_MOVE,
        HOLD_RELEASE,
        CRANE_PICKUP,
        CRANE_RELEASE,
        SHIFT_START,
        SHIFT_END,
        SAMPLE_STATISTICS
    }

    private final Type type;
    private final double time;
    private final Valve valve;
    private final Object data;

    public Event(Type type, double time, Valve valve, Object data) {
        this.type = type;
        this.time = time;
        this.valve = valve;
        this.data = data;
    }

    public Event(Type type, double time) {
        this(type, time, null, null);
    }

    @Override
    public int compareTo(Event other) {
        int timeComparison = Double.compare(this.time, other.time);
        if (timeComparison != 0) {
            return timeComparison;
        }
        // Priority order for simultaneous events
        return Integer.compare(this.type.ordinal(), other.type.ordinal());
    }

    public Type getType() { return type; }
    public double getTime() { return time; }
    public Valve getValve() { return valve; }
    public Object getData() { return data; }

    @Override
    public String toString() {
        return String.format("Event[%s at %.2f, valve=%s]", type, time, valve);
    }
}
