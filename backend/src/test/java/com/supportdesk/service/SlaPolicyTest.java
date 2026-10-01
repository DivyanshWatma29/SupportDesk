package com.supportdesk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.supportdesk.model.Priority;
import com.supportdesk.model.Ticket;
import com.supportdesk.model.TicketStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SlaPolicyTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 30, 12, 0);

    private Ticket ticket(Priority priority, TicketStatus status, LocalDateTime dueAt, LocalDateTime resolvedAt) {
        Ticket ticket = new Ticket();
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setDueAt(dueAt);
        ticket.setResolvedAt(resolvedAt);
        return ticket;
    }

    @Test
    void priorityHoursAreHighFourMediumTwentyFourLowSeventyTwo() {
        assertEquals(4, Priority.HIGH.getSlaHours());
        assertEquals(24, Priority.MEDIUM.getSlaHours());
        assertEquals(72, Priority.LOW.getSlaHours());
    }

    @Test
    void openTicketWithPlentyOfTimeIsOnTrack() {
        Ticket t = ticket(Priority.HIGH, TicketStatus.OPEN, NOW.plusHours(3), null);
        assertEquals(SlaPolicy.ON_TRACK, SlaPolicy.status(t, NOW));
        assertEquals(180L, SlaPolicy.minutesLeft(t, NOW));
    }

    @Test
    void openTicketInLastQuarterOfWindowIsAtRisk() {
        // HIGH window is 240 minutes, so the last 25% is 60 minutes
        Ticket t = ticket(Priority.HIGH, TicketStatus.IN_PROGRESS, NOW.plusMinutes(45), null);
        assertEquals(SlaPolicy.AT_RISK, SlaPolicy.status(t, NOW));
    }

    @Test
    void atRiskStartsExactlyAtTheQuarterMark() {
        Ticket t = ticket(Priority.HIGH, TicketStatus.OPEN, NOW.plusMinutes(60), null);
        assertEquals(SlaPolicy.AT_RISK, SlaPolicy.status(t, NOW));
        Ticket justOutside = ticket(Priority.HIGH, TicketStatus.OPEN, NOW.plusMinutes(61), null);
        assertEquals(SlaPolicy.ON_TRACK, SlaPolicy.status(justOutside, NOW));
    }

    @Test
    void openTicketPastDueIsOverdueWithNegativeMinutes() {
        Ticket t = ticket(Priority.MEDIUM, TicketStatus.OPEN, NOW.minusMinutes(30), null);
        assertEquals(SlaPolicy.OVERDUE, SlaPolicy.status(t, NOW));
        assertEquals(-30L, SlaPolicy.minutesLeft(t, NOW));
    }

    @Test
    void ticketResolvedBeforeDueTimeMetItsSla() {
        Ticket t = ticket(Priority.LOW, TicketStatus.RESOLVED, NOW.plusHours(1), NOW.minusHours(1));
        assertEquals(SlaPolicy.MET, SlaPolicy.status(t, NOW));
        assertNull(SlaPolicy.minutesLeft(t, NOW));
    }

    @Test
    void ticketResolvedAfterDueTimeMissedItsSla() {
        Ticket t = ticket(Priority.HIGH, TicketStatus.RESOLVED, NOW.minusHours(2), NOW.minusHours(1));
        assertEquals(SlaPolicy.MISSED, SlaPolicy.status(t, NOW));
    }

    @Test
    void ticketResolvedExactlyAtDueTimeMetItsSla() {
        Ticket t = ticket(Priority.HIGH, TicketStatus.RESOLVED, NOW, NOW);
        assertEquals(SlaPolicy.MET, SlaPolicy.status(t, NOW));
    }

    @Test
    void ticketWithoutDueTimeHasNoSlaState() {
        Ticket t = ticket(Priority.HIGH, TicketStatus.OPEN, null, null);
        assertNull(SlaPolicy.status(t, NOW));
        assertNull(SlaPolicy.minutesLeft(t, NOW));
    }

    @Test
    void resolvedTicketWithoutResolvedTimeHasNoSlaState() {
        Ticket t = ticket(Priority.HIGH, TicketStatus.RESOLVED, NOW.plusHours(1), null);
        assertNull(SlaPolicy.status(t, NOW));
    }
}
