package com.supportdesk.service;

import com.supportdesk.model.Ticket;
import com.supportdesk.model.TicketStatus;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Works out the SLA (service level agreement) state of a ticket.
 *
 * Open tickets:     ON_TRACK, AT_RISK (last 25% of the time window left) or OVERDUE.
 * Resolved tickets: MET (resolved by the due time) or MISSED (resolved late).
 * Tickets with no due time (created before SLA tracking) have no SLA state.
 */
public final class SlaPolicy {
    public static final String ON_TRACK = "ON_TRACK";
    public static final String AT_RISK = "AT_RISK";
    public static final String OVERDUE = "OVERDUE";
    public static final String MET = "MET";
    public static final String MISSED = "MISSED";

    private static final double AT_RISK_FRACTION = 0.25;

    private SlaPolicy() {
    }

    public static String status(Ticket ticket, LocalDateTime now) {
        if (ticket.getDueAt() == null) {
            return null;
        }
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            if (ticket.getResolvedAt() == null) {
                return null;
            }
            return ticket.getResolvedAt().isAfter(ticket.getDueAt()) ? MISSED : MET;
        }
        if (now.isAfter(ticket.getDueAt())) {
            return OVERDUE;
        }
        long windowMinutes = ticket.getPriority() == null ? 0 : ticket.getPriority().getSlaHours() * 60L;
        long minutesLeft = Duration.between(now, ticket.getDueAt()).toMinutes();
        return minutesLeft <= windowMinutes * AT_RISK_FRACTION ? AT_RISK : ON_TRACK;
    }

    /** Minutes until the due time. Negative when overdue. Null if resolved or there is no due time. */
    public static Long minutesLeft(Ticket ticket, LocalDateTime now) {
        if (ticket.getDueAt() == null || ticket.getStatus() == TicketStatus.RESOLVED) {
            return null;
        }
        return Duration.between(now, ticket.getDueAt()).toMinutes();
    }
}
