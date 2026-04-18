package com.enterprise.ticketing.observability.service.impl;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.domain.AiRunStatus;
import com.enterprise.ticketing.ai.repository.AiRunRepository;
import com.enterprise.ticketing.approval.domain.ApprovalStatus;
import com.enterprise.ticketing.approval.repository.ApprovalRepository;
import com.enterprise.ticketing.observability.dto.DashboardMetricsResponse;
import com.enterprise.ticketing.observability.service.TelemetryService;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.repository.TicketRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultTelemetryService implements TelemetryService {

    private final MeterRegistry meterRegistry;
    private final Tracer tracer;
    private final TicketRepository ticketRepository;
    private final ApprovalRepository approvalRepository;
    private final AiRunRepository aiRunRepository;

    public DefaultTelemetryService(
            MeterRegistry meterRegistry,
            Tracer tracer,
            TicketRepository ticketRepository,
            ApprovalRepository approvalRepository,
            AiRunRepository aiRunRepository
    ) {
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
        this.ticketRepository = ticketRepository;
        this.approvalRepository = approvalRepository;
        this.aiRunRepository = aiRunRepository;
        registerGauges();
    }

    @Override
    public <T> T inSpan(String spanName, Map<String, String> tags, Supplier<T> supplier) {
        Span span = tracer.nextSpan().name(spanName);
        tags.forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                span.tag(key, value);
            }
        });

        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            return supplier.get();
        } catch (RuntimeException exception) {
            span.error(exception);
            throw exception;
        } finally {
            span.end();
        }
    }

    @Override
    public void inSpan(String spanName, Map<String, String> tags, Runnable runnable) {
        inSpan(spanName, tags, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void recordAiOrchestrationResult(boolean success, long durationMs) {
        meterRegistry.counter("ticketing.ai.orchestration.runs", "status", success ? "SUCCESS" : "FAILED").increment();
        Timer.builder("ticketing.ai.orchestration.latency")
                .tag("status", success ? "SUCCESS" : "FAILED")
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMs));
    }

    @Override
    public void recordAiDecisionHandling(boolean requiresApproval, boolean startedWorkflow, long durationMs) {
        meterRegistry.counter(
                "ticketing.workflow.ai.decision.handled",
                "requires_approval", Boolean.toString(requiresApproval),
                "started_workflow", Boolean.toString(startedWorkflow)
        ).increment();
        Timer.builder("ticketing.workflow.ai.decision.latency")
                .tag("requires_approval", Boolean.toString(requiresApproval))
                .tag("started_workflow", Boolean.toString(startedWorkflow))
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMs));
    }

    @Override
    public void recordApprovalWorkflowStarted() {
        meterRegistry.counter("ticketing.workflow.approval.workflows.started").increment();
    }

    @Override
    public void recordApprovalWorkflowCompleted(String result, long durationMs) {
        meterRegistry.counter("ticketing.workflow.approval.workflows.completed", "result", result).increment();
        Timer.builder("ticketing.workflow.approval.workflow.duration")
                .tag("result", result)
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMs));
    }

    @Override
    public void recordApprovalStageOpened(String stageKey) {
        meterRegistry.counter("ticketing.workflow.approval.stage.opened", "stage", stageKey).increment();
    }

    @Override
    public void recordApprovalDecision(String stageKey, ApprovalStatus status, long waitDurationMs, boolean idempotent) {
        meterRegistry.counter(
                "ticketing.workflow.approval.decisions",
                "stage", stageKey,
                "status", status.name(),
                "idempotent", Boolean.toString(idempotent)
        ).increment();
        Timer.builder("ticketing.workflow.approval.wait.duration")
                .tag("stage", stageKey)
                .tag("status", status.name())
                .register(meterRegistry)
                .record(Duration.ofMillis(waitDurationMs));
    }

    @Override
    public void recordApprovalCommand(String action, String result) {
        meterRegistry.counter("ticketing.workflow.approval.commands", "action", action, "result", result).increment();
    }

    @Override
    public void recordApprovalFailure(String phase) {
        meterRegistry.counter("ticketing.workflow.approval.failures", "phase", phase).increment();
    }

    @Override
    public void recordApprovalRetry(String source) {
        meterRegistry.counter("ticketing.workflow.approval.retries", "source", source).increment();
    }

    @Override
    public DashboardMetricsResponse getDashboardMetrics() {
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        Arrays.stream(TicketStatus.values())
                .forEach(status -> statusDistribution.put(status.name(), ticketRepository.countByStatus(status)));

        long orchestrationTotal = aiRunRepository.countByNodeName(AiNodeName.ORCHESTRATION);
        long orchestrationSuccess = aiRunRepository.countByNodeNameAndStatus(AiNodeName.ORCHESTRATION, AiRunStatus.SUCCESS);
        double successRate = orchestrationTotal == 0
                ? 0D
                : orchestrationSuccess * 100D / orchestrationTotal;

        return new DashboardMetricsResponse(
                ticketRepository.count(),
                statusDistribution,
                successRate,
                nullSafe(aiRunRepository.averageLatencyByNodeAndStatus(AiNodeName.ORCHESTRATION, null)),
                nullSafe(aiRunRepository.averageLatencyByNodeAndStatus(AiNodeName.RETRIEVER, AiRunStatus.SUCCESS)),
                nullSafe(approvalRepository.averageDecisionLatencyMs()),
                approvalRepository.countByStatus(ApprovalStatus.PENDING),
                counterValue("ticketing.workflow.approval.failures"),
                counterValue("ticketing.workflow.approval.retries")
        );
    }

    private void registerGauges() {
        Gauge.builder("ticketing.ticket.total", ticketRepository, TicketRepository::count)
                .description("Total number of tickets")
                .register(meterRegistry);
        Arrays.stream(TicketStatus.values()).forEach(status -> Gauge.builder(
                        "ticketing.ticket.status",
                        ticketRepository,
                        repository -> repository.countByStatus(status)
                ).tag("status", status.name()).register(meterRegistry));
        Gauge.builder("ticketing.workflow.approval.pending", approvalRepository, repository -> repository.countByStatus(ApprovalStatus.PENDING))
                .description("Current number of pending approvals")
                .register(meterRegistry);
    }

    private double counterValue(String name, String... tags) {
        Counter counter = meterRegistry.find(name).tags(tags).counter();
        return counter == null ? 0D : counter.count();
    }

    private double nullSafe(Double value) {
        return value == null ? 0D : value;
    }
}
