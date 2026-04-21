package com.enterprise.ticketing.ticket.controller;

import com.enterprise.ticketing.auth.security.JwtAuthenticationFilter;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.dto.CreateTicketRequest;
import com.enterprise.ticketing.ticket.dto.TicketCommentResponse;
import com.enterprise.ticketing.ticket.dto.TicketDetailResponse;
import com.enterprise.ticketing.ticket.dto.TicketListQuery;
import com.enterprise.ticketing.ticket.dto.TicketListResponse;
import com.enterprise.ticketing.ticket.dto.TicketResponse;
import com.enterprise.ticketing.ticket.dto.TicketSummaryResponse;
import com.enterprise.ticketing.ticket.dto.TicketUserSummaryResponse;
import com.enterprise.ticketing.ticket.service.TicketAssignmentService;
import com.enterprise.ticketing.ticket.service.TicketCommentService;
import com.enterprise.ticketing.ticket.service.TicketQueryService;
import com.enterprise.ticketing.ticket.service.TicketService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(
        controllers = TicketController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.enterprise.ticketing.common.handler.GlobalExceptionHandler.class)
class TicketControllerWebMvcTest {

    private static final Instant NOW = Instant.parse("2026-04-19T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private TicketQueryService ticketQueryService;

    @MockBean
    private TicketCommentService ticketCommentService;

    @MockBean
    private TicketAssignmentService ticketAssignmentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createTicketReturnsWrappedTicket() throws Exception {
        TicketResponse response = ticketResponse(101L, "VPN certificate expired", TicketStatus.OPEN);
        when(ticketService.createTicket(eq(new CreateTicketRequest(
                "VPN certificate expired",
                "VPN client reports certificate expired.",
                "VPN",
                TicketPriority.HIGH
        )))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "VPN certificate expired",
                                  "description": "VPN client reports certificate expired.",
                                  "category": "VPN",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_SUCCESS"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(101))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("OPEN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.requester.username").value("alice"));
    }

    @Test
    void createTicketReturnsValidationErrorsForBlankFields() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "description": "",
                                  "category": "VPN",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_VALIDATION_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("must not be blank"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.description").value("must not be blank"));
    }

    @Test
    void listTicketsBindsFiltersAndReturnsWrappedPage() throws Exception {
        TicketListResponse response = new TicketListResponse(
                List.of(ticketSummary(201L, "Approval pending", TicketStatus.WAITING_APPROVAL)),
                1,
                5,
                12,
                3
        );
        when(ticketQueryService.listTickets(any(TicketListQuery.class))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/tickets")
                        .param("keyword", "approval")
                        .param("status", "WAITING_APPROVAL")
                        .param("priority", "URGENT")
                        .param("category", "Finance")
                        .param("requesterId", "7")
                        .param("assigneeId", "8")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "ASC"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(201))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].status").value("WAITING_APPROVAL"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalElements").value(12));

        ArgumentCaptor<TicketListQuery> queryCaptor = ArgumentCaptor.forClass(TicketListQuery.class);
        verify(ticketQueryService).listTickets(queryCaptor.capture());
        TicketListQuery query = queryCaptor.getValue();
        assertEquals("approval", query.getKeyword());
        assertEquals(TicketStatus.WAITING_APPROVAL, query.getStatus());
        assertEquals(TicketPriority.URGENT, query.getPriority());
        assertEquals("Finance", query.getCategory());
        assertEquals(7L, query.getRequesterId());
        assertEquals(8L, query.getAssigneeId());
        assertEquals(1, query.getPage());
        assertEquals(5, query.getSize());
        assertEquals("createdAt", query.getSortBy());
        assertEquals("ASC", query.getSortDirection());
    }

    @Test
    void getTicketDetailReturnsAggregatedPayload() throws Exception {
        when(ticketQueryService.getTicketDetail(301L)).thenReturn(new TicketDetailResponse(
                ticketResponse(301L, "Detail ticket", TicketStatus.IN_PROGRESS),
                List.of(new TicketCommentResponse(401L, "Need more logs", user(2L, "bob"), NOW, NOW)),
                List.of()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/tickets/301"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.ticket.id").value(301))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.ticket.status").value("IN_PROGRESS"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.comments[0].content").value("Need more logs"));
    }

    @Test
    void appendCommentDispatchesContentToService() throws Exception {
        when(ticketCommentService.appendComment(401L, "Attached logs."))
                .thenReturn(new TicketCommentResponse(501L, "Attached logs.", user(1L, "alice"), NOW, NOW));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tickets/401/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Attached logs."
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(501))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("Attached logs."));
    }

    @Test
    void appendCommentReturnsValidationErrorForBlankContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tickets/401/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": " "
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_VALIDATION_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").value("must not be blank"));
    }

    @Test
    void assignTicketDispatchesAssigneeAndNote() throws Exception {
        when(ticketAssignmentService.assignTicket(501L, 9L, "Escalate to VPN team"))
                .thenReturn(ticketResponse(501L, "Assign ticket", TicketStatus.IN_PROGRESS));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tickets/501/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigneeId": 9,
                                  "note": "Escalate to VPN team"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(501));
    }

    @Test
    void updateStatusDispatchesTargetStatusAndReason() throws Exception {
        when(ticketService.updateStatus(601L, TicketStatus.RESOLVED, "Fixed by refreshing certificate"))
                .thenReturn(ticketResponse(601L, "Status ticket", TicketStatus.RESOLVED));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/tickets/601/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "reason": "Fixed by refreshing certificate"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("RESOLVED"));
    }

    private static TicketResponse ticketResponse(Long id, String title, TicketStatus status) {
        return new TicketResponse(
                id,
                title,
                "Description",
                "VPN",
                TicketPriority.HIGH,
                status,
                user(1L, "alice"),
                user(2L, "bob"),
                NOW,
                NOW
        );
    }

    private static TicketSummaryResponse ticketSummary(Long id, String title, TicketStatus status) {
        return new TicketSummaryResponse(
                id,
                title,
                "Finance",
                TicketPriority.URGENT,
                status,
                user(7L, "requester"),
                user(8L, "assignee"),
                NOW,
                NOW
        );
    }

    private static TicketUserSummaryResponse user(Long id, String username) {
        return new TicketUserSummaryResponse(id, username, username.toUpperCase(), "IT");
    }
}
