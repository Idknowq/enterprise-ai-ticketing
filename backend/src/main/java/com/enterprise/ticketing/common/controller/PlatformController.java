package com.enterprise.ticketing.common.controller;

import com.enterprise.ticketing.common.api.OpenApiResultSchemas;
import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.dto.PlatformInfoResponse;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.enterprise.ticketing.config.ApplicationProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Platform", description = "Platform foundation and smoke-check APIs")
@RestController
public class PlatformController {

    private final Environment environment;
    private final ApplicationProperties applicationProperties;

    public PlatformController(Environment environment, ApplicationProperties applicationProperties) {
        this.environment = environment;
        this.applicationProperties = applicationProperties;
    }

    @Operation(summary = "Platform info", description = "Returns project-level foundation metadata for smoke checks.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OpenApiResultSchemas.PlatformInfoResult.class)))
    @GetMapping("${app.api-base-path:/api}/platform/info")
    public Result<PlatformInfoResponse> info() {
        Map<String, Boolean> modules = new LinkedHashMap<>();
        modules.put("auth", applicationProperties.getModules().isAuth());
        modules.put("ticket", applicationProperties.getModules().isTicket());
        modules.put("knowledge", applicationProperties.getModules().isKnowledge());
        modules.put("ai", applicationProperties.getModules().isAi());
        modules.put("workflow", applicationProperties.getModules().isWorkflow());
        modules.put("approval", applicationProperties.getModules().isApproval());
        modules.put("observability", applicationProperties.getModules().isObservability());

        PlatformInfoResponse data = new PlatformInfoResponse(
                "enterprise-ai-ticketing",
                "0.1.0-SNAPSHOT",
                Arrays.asList(environment.getActiveProfiles()),
                applicationProperties.getApiBasePath(),
                modules
        );
        return Result.success(data, TraceIdUtils.currentTraceId());
    }
}
