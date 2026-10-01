package com.supportdesk.service;

import com.supportdesk.dto.AuditLogResponse;
import com.supportdesk.dto.CreateTicketRequest;
import com.supportdesk.dto.DashboardSummary;
import com.supportdesk.dto.TicketDetailResponse;
import com.supportdesk.dto.TicketResponse;
import com.supportdesk.dto.UpdateTicketRequest;
import com.supportdesk.exception.ResourceNotFoundException;
import com.supportdesk.model.AuditLog;
import com.supportdesk.model.Department;
import com.supportdesk.model.Priority;
import com.supportdesk.model.Ticket;
import com.supportdesk.model.TicketStatus;
import com.supportdesk.model.User;
import com.supportdesk.model.UserRole;
import com.supportdesk.repository.AuditLogRepository;
import com.supportdesk.repository.DepartmentRepository;
import com.supportdesk.repository.TicketRepository;
import com.supportdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public TicketService(
            TicketRepository ticketRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        String email = request.requesterEmail().trim().toLowerCase();
        String name = request.requesterName().trim();

        User requester = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(name, email, UserRole.EMPLOYEE, null)));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department " + request.departmentId() + " was not found"));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDepartment(department);
        ticket.setRequester(requester);

        Ticket savedTicket = ticketRepository.save(ticket);
        savedTicket.setTicketNumber("SD-%05d".formatted(savedTicket.getId()));
        Ticket persistedTicket = ticketRepository.save(savedTicket);

        AuditLog initialLog = new AuditLog(
                persistedTicket,
                requester,
                "CREATED",
                null,
                TicketStatus.OPEN.name(),
                "Ticket submitted by " + requester.getName()
        );
        auditLogRepository.save(initialLog);

        return toResponse(persistedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTickets(Long departmentId, TicketStatus status, Priority priority) {
        return ticketRepository.findByFilters(departmentId, status, priority).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketByNumber(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumberWithDetails(ticketNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket " + ticketNumber + " was not found"));
        List<AuditLogResponse> logs = auditLogRepository.findByTicketIdWithUser(ticket.getId()).stream()
                .map(this::toAuditResponse)
                .toList();
        return new TicketDetailResponse(toResponse(ticket), logs);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketById(Long id) {
        Ticket ticket = findTicket(id);
        List<AuditLogResponse> logs = auditLogRepository.findByTicketIdWithUser(ticket.getId()).stream()
                .map(this::toAuditResponse)
                .toList();
        return new TicketDetailResponse(toResponse(ticket), logs);
    }

    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request, String adminUsername) {
        Ticket ticket = findTicket(id);
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.status();

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED && oldStatus != TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (newStatus != TicketStatus.RESOLVED && oldStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(null);
        }
        if (request.adminNote() != null) {
            ticket.setAdminNote(request.adminNote().trim());
        }

        if (request.assignedToId() != null) {
            User assignee = userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User " + request.assignedToId() + " was not found"));
            ticket.setAssignedTo(assignee);
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        User adminUser = adminUsername != null
                ? userRepository.findByEmail(adminUsername).orElse(null)
                : null;

        String action = oldStatus != newStatus ? "STATUS_UPDATED" : "NOTE_UPDATED";
        AuditLog auditLog = new AuditLog(
                savedTicket,
                adminUser,
                action,
                oldStatus.name(),
                newStatus.name(),
                ticket.getAdminNote()
        );
        auditLogRepository.save(auditLog);

        return toResponse(savedTicket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = findTicket(id);
        ticketRepository.delete(ticket);
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        Map<String, Long> departmentCounts = new LinkedHashMap<>();
        for (Department dept : departmentRepository.findAll()) {
            departmentCounts.put(dept.getName(), ticketRepository.countByDepartmentId(dept.getId()));
        }

        LocalDateTime now = LocalDateTime.now();
        long measured = ticketRepository.countByStatusAndResolvedAtIsNotNullAndDueAtIsNotNull(TicketStatus.RESOLVED);
        long onTime = ticketRepository.countResolvedOnTime(TicketStatus.RESOLVED);
        Double slaMetPercent = measured == 0 ? null : Math.round(onTime * 1000.0 / measured) / 10.0;

        List<TicketRepository.ResolutionTimes> resolved = ticketRepository.findResolutionTimes(TicketStatus.RESOLVED);
        Double avgResolutionHours = resolved.isEmpty() ? null : Math.round(
                resolved.stream()
                        .mapToLong(r -> Duration.between(r.getCreatedAt(), r.getResolvedAt()).toMinutes())
                        .average().orElse(0) / 60.0 * 10) / 10.0;

        return new DashboardSummary(
                ticketRepository.count(),
                ticketRepository.countByStatus(TicketStatus.OPEN),
                ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                ticketRepository.countByStatus(TicketStatus.RESOLVED),
                ticketRepository.countByPriority(Priority.HIGH),
                departmentCounts,
                ticketRepository.countByStatusNotAndDueAtBefore(TicketStatus.RESOLVED, now),
                slaMetPercent,
                avgResolutionHours
        );
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket " + id + " was not found"));
    }

    private TicketResponse toResponse(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getDepartment().getId(),
                ticket.getDepartment().getName(),
                ticket.getDepartment().getCode(),
                ticket.getRequester().getName(),
                ticket.getRequester().getEmail(),
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getName() : null,
                ticket.getAdminNote(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getDueAt(),
                ticket.getResolvedAt(),
                SlaPolicy.status(ticket, now),
                SlaPolicy.minutesLeft(ticket, now)
        );
    }

    private AuditLogResponse toAuditResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getOldStatus(),
                log.getNewStatus(),
                log.getNote(),
                log.getChangedBy() != null ? log.getChangedBy().getName() : "System / Requester",
                log.getCreatedAt()
        );
    }
}
