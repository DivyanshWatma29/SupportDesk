package com.supportdesk.dto;

import com.supportdesk.model.Priority;
import com.supportdesk.model.TicketStatus;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketNumber,
        String title,
        String description,
        Priority priority,
        TicketStatus status,
        Long departmentId,
        String departmentName,
        String departmentCode,
        String requesterName,
        String requesterEmail,
        String assignedToName,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime dueAt,
        LocalDateTime resolvedAt,
        String slaStatus,
        Long slaMinutesLeft
) {}
