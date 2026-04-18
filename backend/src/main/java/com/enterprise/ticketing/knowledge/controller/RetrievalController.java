package com.enterprise.ticketing.knowledge.controller;

import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchRequest;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchResponse;
import com.enterprise.ticketing.knowledge.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Knowledge Retrieval", description = "Permission-aware evidence retrieval APIs")
@Validated
@RestController
@RequestMapping("${app.api-base-path:/api}/retrieval")
public class RetrievalController {

    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Operation(summary = "Search knowledge", description = "Retrieve citation-ready knowledge chunks for AI or ticket workflows.")
    @PostMapping("/search")
    public Result<RetrievalSearchResponse> search(@Valid @RequestBody RetrievalSearchRequest request) {
        return Result.success(retrievalService.search(request), TraceIdUtils.currentTraceId());
    }
}
