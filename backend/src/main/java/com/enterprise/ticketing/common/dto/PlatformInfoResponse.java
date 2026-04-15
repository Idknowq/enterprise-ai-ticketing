package com.enterprise.ticketing.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Platform metadata used for smoke check and baseline verification")
public record PlatformInfoResponse(
        @Schema(description = "Service name") String application,
        @Schema(description = "Application version") String version,
        @Schema(description = "Active Spring profiles") List<String> activeProfiles,
        @Schema(description = "API base path") String apiBasePath,
        @Schema(description = "Module flags reserved for future threads") Map<String, Boolean> modules
) {
}

