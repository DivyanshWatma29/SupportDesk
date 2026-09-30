package com.supportdesk.service;

import com.supportdesk.dto.CreateTicketRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock private TicketRepository ticketRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private TicketService ticketService;

    private Department itDepartment;
    private User requester;

    @BeforeEach
    void setUp() {
        itDepartment = new Department("IT Support", "IT");
        itDepartment.setId(1L);

        requester = new User("Asha Sharma", "asha@example.com", UserRole.EMPLOYEE, itDepartment);
        requester.setId(10L);
    }

    @Test
    void createsReadableTicketNumberAndAuditLog() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(requester));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDepartment));

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(42L);
            return ticket;
        });

        TicketResponse response = ticketService.createTicket(new CreateTicketRequest(
                "Asha Sharma", "asha@example.com", 1L,
                "Screen Flicker Issue", "Laptop screen flickers continuously.", Priority.HIGH
        ));

        assertEquals("SD-00042", response.ticketNumber());
        assertEquals("Screen Flicker Issue", response.title());
        assertEquals("IT Support", response.departmentName());
        verify(ticketRepository, times(2)).save(any(Ticket.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void updatesTicketStatusAndLogsAuditTrail() {
        Ticket ticket = new Ticket();
        ticket.setId(42L);
        ticket.setTicketNumber("SD-00042");
        ticket.setTitle("Network slow");
        ticket.setDescription("VPN disconnection issues.");
        ticket.setPriority(Priority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDepartment(itDepartment);
        ticket.setRequester(requester);

        when(ticketRepository.findById(42L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketResponse updated = ticketService.updateTicket(
                42L,
                new UpdateTicketRequest(TicketStatus.RESOLVED, "Reconfigured VPN gateway.", null),
                "admin@supportdesk.local"
        );

        assertEquals(TicketStatus.RESOLVED, updated.status());
        assertEquals("Reconfigured VPN gateway.", updated.adminNote());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void tracksTicketByNumberWithAuditHistory() {
        Ticket ticket = new Ticket();
        ticket.setId(42L);
        ticket.setTicketNumber("SD-00042");
        ticket.setTitle("Wi-Fi down");
        ticket.setDescription("Cannot connect to office Wi-Fi.");
        ticket.setPriority(Priority.HIGH);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDepartment(itDepartment);
        ticket.setRequester(requester);

        AuditLog log = new AuditLog(ticket, requester, "CREATED", null, "OPEN", "Submitted");

        when(ticketRepository.findByTicketNumberWithDetails("SD-00042")).thenReturn(Optional.of(ticket));
        when(auditLogRepository.findByTicketIdWithUser(42L)).thenReturn(List.of(log));

        TicketDetailResponse detail = ticketService.getTicketByNumber("SD-00042");

        assertNotNull(detail);
        assertEquals("SD-00042", detail.ticket().ticketNumber());
        assertEquals(1, detail.timeline().size());
        assertEquals("CREATED", detail.timeline().get(0).action());
    }

    @Test
    void throwsResourceNotFoundWhenTicketNotFound() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicketById(999L));
    }
}
