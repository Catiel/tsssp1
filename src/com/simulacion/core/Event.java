package com.simulacion.core;

public abstract class Event implements Comparable<Event> {
    protected double scheduledTime;
    protected int priority;
    protected String description;

    public Event(double scheduledTime, int priority, String description) {
        this.scheduledTime = scheduledTime;
        this.priority = priority;
        this.description = description;
    }

    public abstract void execute();

    public double getScheduledTime() {
        return scheduledTime;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(Event other) {
        int timeComparison = Double.compare(this.scheduledTime, other.scheduledTime);
        if (timeComparison != 0) {
            return timeComparison;
        }
        return Integer.compare(this.priority, other.priority);
    }
}
