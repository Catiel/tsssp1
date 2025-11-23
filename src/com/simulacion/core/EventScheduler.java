package com.simulacion.core;

import java.util.PriorityQueue;

public class EventScheduler {
    private final PriorityQueue<Event> eventList;
    private final SimulationClock clock;

    public EventScheduler(SimulationClock clock) {
        this.eventList = new PriorityQueue<>();
        this.clock = clock;
    }

    public void scheduleEvent(Event event) {
        eventList.add(event);
    }

    public Event getNextEvent() {
        return eventList.poll();
    }

    public boolean hasEvents() {
        return !eventList.isEmpty();
    }

    public void clear() {
        eventList.clear();
    }

    public int getEventCount() {
        return eventList.size();
    }
}
