package com.supportdesk.dto;

import java.util.Map;

public record DashboardSummary(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long resolvedTickets,
        long highPriorityTickets,
        Map<String, Long> ticketsByDepartment,
        long overdueTickets,
        Double slaMetPercent,
        Double avgResolutionHours
) {}
