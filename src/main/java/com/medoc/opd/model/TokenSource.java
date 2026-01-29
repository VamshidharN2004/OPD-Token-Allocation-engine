package com.medoc.opd.model;

public enum TokenSource {
    EMERGENCY(1),
    PAID_PREMIUM(2),
    FOLLOW_UP(3),
    ONLINE(4),
    WALK_IN(5);

    private final int priorityLevel;

    TokenSource(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }
}
