package com.enterprise.ticketing.ticket.controller;

import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.enterprise.ticketing.ticket.dto.AppendTicketCommentRequest;
import com.enterprise.ticketing.ticket.dto.AssignTicketRequest;
import com.enterprise.ticketing.ticket.dto.CreateTicketRequest;
import com.enterprise.ticketing.ticket.dto.TicketCommentResponse;
import com.enterprise.ticketing.ticket.dto.TicketDetailResponse;
import com.enterprise.ticketing.ticket.dto.TicketListQuery;
import com.enterprise.ticketing.ticket.dto.TicketListResponse;
import com.enterprise.ticketing.ticket.dto.TicketResponse;
import com.enterprise.ticketing.ticket.dto.UpdateTicketStatusRequest;
import com.enterprise.ticketing.ticket.service.TicketAssignmentService;
import com.enterprise.ticketing.ticket.service.TicketCommentService;
import com.enterprise.ticketing.ticket.service.TicketQueryService;
import com.enterprise.ticketing.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ticket", description = "Ticket core APIs")
@Validated
@RestController
@RequestMapping("${app.api-base-path:/api}/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketQueryService ticketQueryService;
    private final TicketCommentService ticketCommentService;
    private final TicketAssignmentService ticketAssignmentService;

    public TicketController(
            TicketService ticketService,
            TicketQueryService ticketQueryService,
            TicketCommentService ticketCommentService,
            TicketAssignmentService ticketAssignmentService
    ) {
        this.ticketService = ticketService;
        this.ticketQueryService = ticketQueryService;
        this.ticketCommentService = ticketCommentService;
        this.ticketAssignmentService = ticketAssignmentService;
    }

    @Operation(summary = "Create ticket", description = "Create a new ticket owned by the current authenticated user.")
    @PostMapping
    public Result<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return Result.success(ticketService.createTicket(request), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "List tickets", description = "Query tickets with permission-aware filtering and pagination.")
    @GetMapping
    public Result<TicketListResponse> listTickets(@Valid @ModelAttribute TicketListQuery query) {
        return Result.success(ticketQueryService.listTickets(query), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Get ticket detail", description = "Return ticket base info, comments, and full timeline.")
    @GetMapping("/{id}")
    public Result<TicketDetailResponse> getTicketDetail(@PathVariable("id") Long ticketId) {
        return Result.success(ticketQueryService.getTicketDetail(ticketId), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Append ticket comment", description = "Append a supplementary comment to an existing ticket.")
    @PostMapping("/{id}/comments")
    public Result<TicketCommentResponse> appendComment(
            @PathVariable("id") Long ticketId,
            @Valid @RequestBody AppendTicketCommentRequest request
    ) {
        return Result.success(ticketCommentService.appendComment(ticketId, request.content()), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Assign ticket", description = "Assign a ticket to a support agent or admin.")
    @PostMapping("/{id}/assign")
    public Result<TicketResponse> assignTicket(
            @PathVariable("id") Long ticketId,
            @Valid @RequestBody AssignTicketRequest request
    ) {
        return Result.success(
                ticketAssignmentService.assignTicket(ticketId, request.assigneeId(), request.note()),
                TraceIdUtils.currentTraceId()
        );
    }

    @Operation(summary = "Update ticket status", description = "Manually update the ticket status through the domain state machine.")
    @PostMapping("/{id}/status")
    public Result<TicketResponse> updateStatus(
            @PathVariable("id") Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return Result.success(
                ticketService.updateStatus(ticketId, request.status(), request.reason()),
                TraceIdUtils.currentTraceId()
        );
    }
}
