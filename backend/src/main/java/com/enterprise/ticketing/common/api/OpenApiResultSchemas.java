package com.enterprise.ticketing.common.api;

import com.enterprise.ticketing.approval.dto.PendingApprovalResponse;
import com.enterprise.ticketing.auth.dto.CurrentUserResponse;
import com.enterprise.ticketing.common.dto.PlatformInfoResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentListResponse;
import com.enterprise.ticketing.observability.dto.DashboardMetricsResponse;
import com.enterprise.ticketing.ticket.dto.TicketListResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public final class OpenApiResultSchemas {

    private OpenApiResultSchemas() {
    }

    @Schema(name = "PlatformInfoResult")
    public record PlatformInfoResult(
            boolean success,
            String code,
            String message,
            PlatformInfoResponse data,
            Instant timestamp,
            String traceId
    ) {
    }

    @Schema(name = "CurrentUserResult")
    public record CurrentUserResult(
            boolean success,
            String code,
            String message,
            CurrentUserResponse data,
            Instant timestamp,
            String traceId
    ) {
    }

    @Schema(name = "DashboardMetricsResult")
    public record DashboardMetricsResult(
            boolean success,
            String code,
            String message,
            DashboardMetricsResponse data,
            Instant timestamp,
            String traceId
    ) {
    }

    @Schema(name = "DocumentListResult")
    public record DocumentListResult(
            boolean success,
            String code,
            String message,
            DocumentListResponse data,
            Instant timestamp,
            String traceId
    ) {
    }

    @Schema(name = "TicketListResult")
    public record TicketListResult(
            boolean success,
            String code,
            String message,
            TicketListResponse data,
            Instant timestamp,
            String traceId
    ) {
    }

    @Schema(name = "PendingApprovalListResult")
    public record PendingApprovalListResult(
            boolean success,
            String code,
            String message,
            @ArraySchema(schema = @Schema(implementation = PendingApprovalResponse.class))
            List<PendingApprovalResponse> data,
            Instant timestamp,
            String traceId
    ) {
    }
}
