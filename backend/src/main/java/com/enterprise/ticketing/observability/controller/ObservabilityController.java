package com.enterprise.ticketing.observability.controller;

import com.enterprise.ticketing.common.api.OpenApiResultSchemas;
import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.enterprise.ticketing.observability.dto.DashboardMetricsResponse;
import com.enterprise.ticketing.observability.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Observability", description = "Dashboard and monitoring data APIs")
@RestController
@RequestMapping("${app.api-base-path:/api}/observability")
public class ObservabilityController {

    private final TelemetryService telemetryService;

    public ObservabilityController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Operation(summary = "Get dashboard metrics", description = "Aggregate workflow, approval, AI, and ticket metrics for the monitoring page.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OpenApiResultSchemas.DashboardMetricsResult.class)))
    @GetMapping("/dashboard")
    public Result<DashboardMetricsResponse> dashboard() {
        return Result.success(telemetryService.getDashboardMetrics(), TraceIdUtils.currentTraceId());
    }
}
