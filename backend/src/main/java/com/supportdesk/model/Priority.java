package com.supportdesk.model;

/**
 * Ticket priority. Each priority has a service level target: the number of hours
 * support has to resolve a ticket of that priority.
 */
public enum Priority {
    LOW(72),
    MEDIUM(24),
    HIGH(4);

    private final int slaHours;

    Priority(int slaHours) {
        this.slaHours = slaHours;
    }

    public int getSlaHours() {
        return slaHours;
    }
}
