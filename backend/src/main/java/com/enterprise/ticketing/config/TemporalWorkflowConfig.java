package com.enterprise.ticketing.config;

import com.enterprise.ticketing.workflow.ApprovalWorkflow;
import com.enterprise.ticketing.workflow.activity.ApprovalWorkflowActivities;
import com.enterprise.ticketing.workflow.impl.ApprovalWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkflowConfig {

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs workflowServiceStubs(ApplicationProperties applicationProperties) {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(applicationProperties.getTemporal().getHost() + ":" + applicationProperties.getTemporal().getPort())
                        .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient(
            WorkflowServiceStubs workflowServiceStubs,
            ApplicationProperties applicationProperties
    ) {
        return WorkflowClient.newInstance(
                workflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(applicationProperties.getTemporal().getNamespace())
                        .build()
        );
    }

    @Bean(destroyMethod = "shutdown")
    public WorkerFactory workerFactory(
            WorkflowClient workflowClient,
            ApprovalWorkflowActivities approvalWorkflowActivities,
            ApplicationProperties applicationProperties
    ) {
        WorkerFactory workerFactory = WorkerFactory.newInstance(workflowClient);
        Worker worker = workerFactory.newWorker(applicationProperties.getTemporal().getTaskQueue());
        worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
        worker.registerActivitiesImplementations(approvalWorkflowActivities);
        workerFactory.start();
        return workerFactory;
    }
}
