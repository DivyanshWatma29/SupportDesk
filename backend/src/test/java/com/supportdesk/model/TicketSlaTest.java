package com.supportdesk.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TicketSlaTest {

    @Test
    void newTicketGetsDueTimeFromItsPriority() {
        for (Priority priority : Priority.values()) {
            Ticket ticket = new Ticket();
            ticket.setPriority(priority);

            ticket.onCreate();

            assertNotNull(ticket.getDueAt());
            Duration window = Duration.between(ticket.getCreatedAt(), ticket.getDueAt());
            assertEquals(priority.getSlaHours(), window.toHours(), "SLA window for " + priority);
        }
    }

    @Test
    void existingDueTimeIsNotOverwritten() {
        LocalDateTime custom = LocalDateTime.of(2030, 1, 1, 9, 0);
        Ticket ticket = new Ticket();
        ticket.setPriority(Priority.HIGH);
        ticket.setDueAt(custom);

        ticket.onCreate();

        assertEquals(custom, ticket.getDueAt());
    }
}
